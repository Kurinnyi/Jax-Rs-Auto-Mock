package ua.kurinnyi.jaxrs.auto.mock.jersey

import org.apache.catalina.Context
import org.apache.catalina.startup.Tomcat
import org.apache.tomcat.util.descriptor.web.FilterDef
import org.apache.tomcat.util.descriptor.web.FilterMap
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer
import org.reflections.Reflections
import ua.kurinnyi.jaxrs.auto.mock.DependenciesRegistry
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.filters.BufferingFilter
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.jersey.groups.GroupResourceImpl
import ua.kurinnyi.jaxrs.auto.mock.mocks.AutoDiscoveryOfStubDefinitions
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus
import ua.kurinnyi.jaxrs.auto.mock.recorder.RecordSaver
import ua.kurinnyi.jaxrs.auto.mock.recorder.ResponseDecoder
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableFilesLoader
import java.io.File
import javax.servlet.Filter
import kotlin.reflect.KClass


class StubServer {

    private val reflections = Reflections()

    private var port = 8080
    private var useJerseyDeserialization = false
    private val packagesToScan = mutableListOf<String>()
    private val classesToRegister = HashSet<Class<*>>()
    private val stubDefinitions  = mutableListOf<StubsDefinition>()
    private var autoDiscoveryOfStubDefinitions  = true

    private var ignoredResources: Set<KClass<*>> = emptySet()

    private var enabledByDefaultGroups:List<String> = emptyList()

    fun setPort(port: Int): StubServer = this.apply{
        this.port = port
    }

    fun addPackageToScanForPorviders(packageName:String): StubServer = this.apply {
        packagesToScan.add(packageName)
    }

    fun useJerseyDeserializationForYamlStubs(): StubServer = this.apply {
        useJerseyDeserialization = true
    }

    fun addProviderClassToRegister(clazz: Class<*>): StubServer = this.apply {
        classesToRegister.add(clazz)
    }

    fun addStubDefinition(stubDefinition: StubsDefinition): StubServer = this.apply {
        stubDefinitions.add(stubDefinition)
    }

    fun disableAutoDiscoveryOfStubDefinition(): StubServer = this.apply {
        autoDiscoveryOfStubDefinitions = false
    }

    fun customProxyConfiguration(proxyConfiguration:ProxyConfiguration): StubServer = this.apply {
        JerseyDependenciesRegistry.proxyConfiguration = proxyConfiguration
    }

    fun ignoreResources(ignoredResources:Set<KClass<*>>): StubServer = this.apply {
        this.ignoredResources = ignoredResources
    }

    fun defaultBodyProvider(bodyProvider:BodyProvider): StubServer = this.apply {
        JerseyDependenciesRegistry.defaultBodyProvider = bodyProvider
    }

    fun enableGroupsOnStart(vararg groupNames: String): StubServer = this.apply {
        enabledByDefaultGroups = groupNames.toList()
    }

    fun setRecordsSaver(saver:RecordSaver): StubServer = this.also {
        JerseyDependenciesRegistry.recordSaver = saver
    }

    fun setYamlFilesLoader(loader:SerializableFilesLoader): StubServer = this.apply {
        JerseyDependenciesRegistry.serializableFilesLoader = loader
    }

    fun addHttpResponseDecoder(decoder:ResponseDecoder): StubServer = this.also {
        JerseyDependenciesRegistry.responseDecoders += decoder
    }

    fun getDependenciesRegistry(): DependenciesRegistry {
        return JerseyDependenciesRegistry
    }

    fun start() {
        val tomcat = Tomcat()
        tomcat.setPort(port)
        addStubDefinition(GroupResourceImpl())
        addStubDefinition(JerseyDependenciesRegistry.serializableStubsDefinitionLoader)
        JerseyDependenciesRegistry.stubDefinitions = getStubDefinitions()
        enabledByDefaultGroups.forEach{ JerseyDependenciesRegistry.groupSwitchService().switchGroupStatus(it, GroupStatus.ACTIVE) }
        getResourceInterfacesToContextMapping(ignoredResources).forEach { contextPath, interfaces ->
            addContext(tomcat, interfaces, contextPath)
        }
        tomcat.start()
        tomcat.server.await()
    }

    private fun addContext(tomcat: Tomcat, interfacesToStub: List<Class<*>>, contextPath:String) {
        val context: Context = tomcat.addWebapp(contextPath, File(".").absolutePath)
        val resourceLoader = ResourceConfig()
        registerResources(resourceLoader, interfacesToStub)
        resourceLoader.register(JerseyDependenciesRegistry.jerseyInternalsFilter)
        resourceLoader.register(NotFoundExceptionMapper::class.java)
        resourceLoader.register(StubNotFoundExceptionMapper::class.java)
        registerCustomProviders(resourceLoader)
        addJerseyServlet(resourceLoader, context)
        addFilter(context, BufferingFilter())
        addFilter(context, JerseyDependenciesRegistry.contextSaveFilter())
        addFilter(context, JerseyDependenciesRegistry.responseIntersectingFilter)
        context.addServletMapping("/*", "jersey-container-servlet")
    }

    private fun registerResources(resourceLoader: ResourceConfig, interfacesToStub: List<Class<*>>) {
        val proxyFactor = JerseyDependenciesRegistry.proxyInstanceFactory()
        val proxyInstances = interfacesToStub.map { resource -> proxyFactor.createMockInstanceForInterface(resource) }
        resourceLoader.registerInstances(proxyInstances.toSet())
    }

    private fun getResourceInterfacesToContextMapping(ignoredResources:Set<KClass<*>>): Map<String, List<Class<*>>> {
        return AutoDiscoveryOfResourceInterfaces(reflections, ignoredResources).getResourceInterfacesToContextMapping()
    }

    private fun addJerseyServlet(resourceLoader: ResourceConfig, context: Context) {
        val resourceConfig = ResourceConfig.forApplication(resourceLoader)
        val servletContainer = ServletContainer(resourceConfig)
        Tomcat.addServlet(context, "jersey-container-servlet", servletContainer)
    }

    private fun registerCustomProviders(resourceLoader: ResourceConfig) {
        resourceLoader.packages(* packagesToScan.toTypedArray())
        resourceLoader.registerClasses(classesToRegister)
    }

    private fun getStubDefinitions(): List<StubsDefinition> {
        if (autoDiscoveryOfStubDefinitions)
            return stubDefinitions + AutoDiscoveryOfStubDefinitions(reflections).getStubDefinitions()
        else
            return stubDefinitions
    }

    private fun addFilter(context: Context, filterInstance:Filter) {
        val def = FilterDef().apply {
            filterName = filterInstance.javaClass.simpleName
            filter = filterInstance
            context.addFilterDef(this)
        }
        FilterMap().apply {
            filterName = def.filterName
            addURLPattern("/*")
            context.addFilterMap(this)
        }
    }
}
