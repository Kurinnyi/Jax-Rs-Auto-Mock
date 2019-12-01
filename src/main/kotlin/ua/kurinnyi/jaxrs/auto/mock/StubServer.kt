package ua.kurinnyi.jaxrs.auto.mock

import org.apache.catalina.Context
import org.apache.catalina.startup.Tomcat
import org.apache.tomcat.util.descriptor.web.FilterDef
import org.apache.tomcat.util.descriptor.web.FilterMap
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer
import org.reflections.Reflections
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.ExtractingBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.FileBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.JacksonBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.endpoint.GroupResourceImpl
import ua.kurinnyi.jaxrs.auto.mock.filters.BufferingFilter
import ua.kurinnyi.jaxrs.auto.mock.filters.ContextSaveFilter
import ua.kurinnyi.jaxrs.auto.mock.filters.ResponseIntersectingFilter
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.NothingMatchedProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.kotlin.*
import ua.kurinnyi.jaxrs.auto.mock.model.GroupStatus
import ua.kurinnyi.jaxrs.auto.mock.yaml.ResponseFromStubCreator
import ua.kurinnyi.jaxrs.auto.mock.yaml.YamlMethodStubsLoader
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

    private var proxyConfiguration:ProxyConfiguration  = NothingMatchedProxyConfiguration()

    private var ignoredResources: Set<KClass<*>> = emptySet()

    private var defaultBodyProvider: BodyProvider = JacksonBodyProvider
    private var defaultExtractingBodyProvider:ExtractingBodyProvider? = null

    private var enabledByDefaultGroups:List<String> = emptyList()

    fun setPort(port: Int): StubServer = this.apply{
        this.port = port
    }

    fun addPackageToScanForPorviders(packageName:String):StubServer = this.apply {
        packagesToScan.add(packageName)
    }

    fun useJerseyDeserializationForYamlStubs():StubServer = this.apply {
        useJerseyDeserialization = true
    }

    fun addProviderClassToRegister(clazz: Class<*>):StubServer = this.apply {
        classesToRegister.add(clazz)
    }

    fun addStubDefinition(stubDefinition: StubsDefinition):StubServer = this.apply {
        stubDefinitions.add(stubDefinition)
    }

    fun disableAutoDiscoveryOfStubDefinition():StubServer = this.apply {
        autoDiscoveryOfStubDefinitions = false
    }

    fun customProxyConfiguration(proxyConfiguration:ProxyConfiguration):StubServer = this.apply {
        this.proxyConfiguration = proxyConfiguration
    }

    fun ignoreResources(ignoredResources:Set<KClass<*>>):StubServer = this.apply {
        this.ignoredResources = ignoredResources
    }

    fun defaultBodyProvider(bodyProvider:BodyProvider):StubServer = this.apply {
        this.defaultBodyProvider = bodyProvider
    }

    fun defaultExtractingBodyProvider(bodyProvider:ExtractingBodyProvider):StubServer = this.apply {
        this.defaultExtractingBodyProvider = bodyProvider
    }

    fun enableGroupsOnStart(vararg groupNames: String):StubServer = this.apply {
        this.enabledByDefaultGroups = groupNames.toList()
    }

    fun start() {
        val tomcat = Tomcat()
        tomcat.setPort(port)
        val methodInvocationHandler = instantiateCommonDependencies()
        enabledByDefaultGroups.forEach{ GroupSwitchService.switchGroupStatus(it, GroupStatus.ACTIVE) }
        getResourceInterfacesToContextMapping(ignoredResources).forEach { contextPath, interfaces ->
            addContext(tomcat, interfaces, contextPath, methodInvocationHandler)
        }
        tomcat.start()
        tomcat.server.await()
    }

    private fun addContext(tomcat: Tomcat, interfacesToStub: List<Class<*>>, contextPath:String, methodHandler: MethodInvocationHandler) {
        val context: Context = tomcat.addWebapp(contextPath, File(".").absolutePath)
        val resourceLoader = ResourceLoaderOfProxyInstances(interfacesToStub, methodHandler)
        resourceLoader.register(JerseyInternalsFilter::class.java)
        resourceLoader.register(NotFoundExceptionMapper::class.java)
        resourceLoader.register(StubNotFoundExceptionMapper::class.java)
        registerCustomProviders(resourceLoader)
        addJerseyServlet(resourceLoader, context)
        addFilter(context, BufferingFilter())
        addFilter(context, ContextSaveFilter())
        addFilter(context, ResponseIntersectingFilter())
        context.addServletMapping("/*", "jersey-container-servlet")
    }

    private fun getResourceInterfacesToContextMapping(ignoredResources:Set<KClass<*>>): Map<String, List<Class<*>>> {
        return AutoDiscoveryOfResourceInterfaces(reflections, ignoredResources).getResourceInterfacesToContextMapping()
    }

    private fun addJerseyServlet(resourceLoader: ResourceLoaderOfProxyInstances, context: Context) {
        val resourceConfig = ResourceConfig.forApplication(resourceLoader)
        val servletContainer = ServletContainer(resourceConfig)
        Tomcat.addServlet(context, "jersey-container-servlet", servletContainer)
    }

    private fun registerCustomProviders(resourceLoader: ResourceLoaderOfProxyInstances) {
        resourceLoader.packages(* packagesToScan.toTypedArray())
        resourceLoader.registerClasses(classesToRegister)
    }

    private fun instantiateCommonDependencies(): MethodInvocationHandler {
        val groupEndpoint = GroupResourceImpl()
        val stubDefinitions: List<StubsDefinition> = listOf(groupEndpoint) + getStubDefinitions()
        ResponseFromStubCreator.useJerseyDeserialization = useJerseyDeserialization
        val methodStubsLoader = CompositeMethodStubLoader(
                KotlinMethodStubsLoader(stubDefinitions, proxyConfiguration),
                YamlMethodStubsLoader())
        GroupSwitchService.loader = methodStubsLoader

        JsonUtils.defaultJsonBodyProvider = defaultBodyProvider
        JsonUtils.defaultExtractingJsonBodyProvider = defaultExtractingBodyProvider?: FileBodyProvider(defaultBodyProvider)
        return MethodInvocationHandler(methodStubsLoader, proxyConfiguration)
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
