package ua.kurinnyi.jaxrs.auto.mock.jersey

import org.apache.catalina.Context
import org.apache.catalina.startup.Tomcat
import org.apache.tomcat.util.descriptor.web.FilterDef
import org.apache.tomcat.util.descriptor.web.FilterMap
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer
import org.reflections.Reflections
import ua.kurinnyi.jaxrs.auto.mock.DependenciesRegistry
import ua.kurinnyi.jaxrs.auto.mock.extensions.*
import ua.kurinnyi.jaxrs.auto.mock.extensions.defaul.ByPackageContextPathConfiguration
import ua.kurinnyi.jaxrs.auto.mock.filters.BufferingFilter
import ua.kurinnyi.jaxrs.auto.mock.jersey.groups.GroupConfigurationResourceImpl
import ua.kurinnyi.jaxrs.auto.mock.mocks.AutoDiscoveryOfStubDefinitions
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus
import java.io.File
import javax.servlet.Filter
import kotlin.reflect.KClass


class StubServer {

    private val reflections = Reflections()
    private var contextPathsConfiguration:ContextPathsConfiguration = ByPackageContextPathConfiguration()

    private val defaultContextPath = "/"
    private var port = 8080
    private val packagesToScan = mutableListOf<String>()
    private val classesToRegister = mutableSetOf<Class<*>>()
    private val stubDefinitions = mutableListOf<StubsDefinition>()
    private var autoDiscoveryOfStubDefinitions = true
    private val ignoredResources = mutableSetOf<Class<*>>()
    private val enabledByDefaultGroups = mutableListOf<String>()
    private var dependencyRegistryReadyCallback: (DependenciesRegistry) -> Unit = {}

    fun onPort(port: Int): StubServer = this.apply {
        this.port = port
    }

    fun addPackageToScanForProviders(packageName: String): StubServer = this.apply {
        packagesToScan.add(packageName)
    }

    fun addProviderClassToRegister(clazz: Class<*>): StubServer = this.apply {
        classesToRegister.add(clazz)
    }

    fun addStubDefinition(stubDefinition: StubsDefinition): StubServer = this.apply {
        stubDefinitions.add(stubDefinition)
    }

    fun addGroupToEnableOnStart(groupName: String): StubServer = this.apply {
        enabledByDefaultGroups.add(groupName)
    }

    fun addResourceToIgnore(ignoredResource: KClass<*>): StubServer = this.apply {
        ignoredResources.add(ignoredResource.java)
    }

    fun addResourceToIgnore(ignoredResource: Class<*>): StubServer = this.apply {
        ignoredResources.add(ignoredResource)
    }

    fun addHttpResponseDecoder(decoderHttp: HttpResponseDecoder): StubServer = this.also {
        JerseyDependenciesRegistry.httpResponseDecoders += decoderHttp
    }

    fun withDisabledAutoDiscoveryOfStubDefinition(): StubServer = this.apply {
        autoDiscoveryOfStubDefinitions = false
    }

    fun withContextPathConfiguration(contextPathsConfiguration: ContextPathsConfiguration): StubServer = this.apply {
        this.contextPathsConfiguration = contextPathsConfiguration
    }

    fun withProxyConfiguration(proxyConfiguration: ProxyConfiguration): StubServer = this.also {
        JerseyDependenciesRegistry.proxyConfiguration = proxyConfiguration
    }

    fun withDefaultResponseBodyProvider(responseBodyProvider: ResponseBodyProvider): StubServer = this.also {
        JerseyDependenciesRegistry.defaultResponseBodyProvider = responseBodyProvider
    }

    fun withRecordsSaver(saver: RecordSaver): StubServer = this.also {
        JerseyDependenciesRegistry.recordSaver = saver
    }

    fun withSerialisedMocksFilesLoader(filesLoader: SerialisedMocksFilesLoader): StubServer = this.also {
        JerseyDependenciesRegistry.serialisedMocksFilesLoader = filesLoader
    }

    fun onDependenciesRegistryReady(callback: (DependenciesRegistry) -> Unit): StubServer = this.apply {
       dependencyRegistryReadyCallback = callback
    }

    fun start() {
        val tomcat = Tomcat()
        tomcat.setPort(port)
        addStubDefinition(GroupConfigurationResourceImpl())
        addStubDefinition(JerseyDependenciesRegistry.serializableMocksLoader)
        JerseyDependenciesRegistry.stubDefinitions = getStubDefinitions()
        enabledByDefaultGroups.forEach { JerseyDependenciesRegistry.groupSwitchService().switchGroupStatus(it, GroupStatus.ACTIVE) }
        getResourceInterfacesToContextMapping(ignoredResources).forEach { contextPath, interfaces ->
            addContext(tomcat, interfaces, contextPath)
        }
        dependencyRegistryReadyCallback(JerseyDependenciesRegistry)
        tomcat.start()
        tomcat.server.await()
    }

    private fun addContext(tomcat: Tomcat, interfacesToMock: List<Class<*>>, contextPath: String) {
        val context: Context = tomcat.addWebapp(contextPath, File(".").absolutePath)
        val resourceLoader = ResourceConfig()
        registerResources(resourceLoader, interfacesToMock)
        resourceLoader.register(JerseyDependenciesRegistry.jerseyInternalsFilter)
        resourceLoader.register(NotFoundExceptionMapper::class.java)
        resourceLoader.register(MockNotFoundExceptionMapper::class.java)
        registerCustomProviders(resourceLoader)
        addJerseyServlet(resourceLoader, context)
        addFilter(context, BufferingFilter())
        addFilter(context, JerseyDependenciesRegistry.httpRequestResponseHolder())
        addFilter(context, JerseyDependenciesRegistry.responseIntersectingFilter)
        context.addServletMapping("/*", "jersey-container-servlet")
    }

    private fun registerResources(resourceLoader: ResourceConfig, interfacesToMock: List<Class<*>>) {
        val proxyFactor = JerseyDependenciesRegistry.proxyInstanceFactory()
        val proxyInstances = interfacesToMock.map { resource -> proxyFactor.createMockInstanceForInterface(resource) }
        resourceLoader.registerInstances(proxyInstances.toSet())
    }

    private fun getResourceInterfacesToContextMapping(ignoredResources: Set<Class<*>>): Map<String, List<Class<*>>> {
        return AutoDiscoveryOfResourceInterfaces(reflections, ignoredResources).getInterfacesToMock()
                .groupBy { clazz -> contextPathsConfiguration.getContextPathsForResource(clazz) ?: defaultContextPath }
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

    private fun addFilter(context: Context, filterInstance: Filter) {
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
