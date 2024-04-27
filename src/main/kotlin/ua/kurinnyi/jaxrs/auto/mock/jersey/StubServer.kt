package ua.kurinnyi.jaxrs.auto.mock.jersey

import jakarta.servlet.Filter
import org.apache.catalina.Context
import org.apache.catalina.startup.Tomcat
import org.apache.tomcat.util.descriptor.web.FilterDef
import org.apache.tomcat.util.descriptor.web.FilterMap
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer
import org.slf4j.LoggerFactory
import ua.kurinnyi.jaxrs.auto.mock.DependenciesRegistry
import ua.kurinnyi.jaxrs.auto.mock.extensions.*
import ua.kurinnyi.jaxrs.auto.mock.extensions.defaul.*
import ua.kurinnyi.jaxrs.auto.mock.filters.BufferingFilter
import ua.kurinnyi.jaxrs.auto.mock.filters.HttpRequestResponseHolder
import ua.kurinnyi.jaxrs.auto.mock.jersey.groups.GroupConfigurationResourceImpl
import ua.kurinnyi.jaxrs.auto.mock.mocks.AutoDiscoveryOfStubDefinitions
import ua.kurinnyi.jaxrs.auto.mock.mocks.SerialisationUtils
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus
import java.io.File
import kotlin.reflect.KClass

/**
 * This class is an entry point to construct and launch mock server.
 * Use it's methods to amend behaviour of the system and use [start] method to launch it.
 * It works in a next way:
 * - After the start the [DependenciesRegistry] is constructed and the [onDependenciesRegistryReady] callback is executed.
 * - Each interface in classpath annotated with [jakarta.ws.rs.Path] which is not explicitly disabled, is autodiscover instantiated and registered
 * in Jersey runtime.
 * - JAX-RS providers are found and registered in Jersey runtime.
 * - Tomcat server with Jersey in launched.
 * - If not specified the opposite, all the instances of [StubsDefinition] in classpath are auto discovered and executed for the mocks.
 * - Server tries to match incoming requests with mocks and use their responses.
 */
class StubServer {
    private val logger = LoggerFactory.getLogger(StubServer::class.java)
    private var contextPathsConfiguration:ContextPathsConfiguration = ByPackageContextPathConfiguration()

    private val defaultContextPath = ""
    private var port = 8080
    private val packagesToScan = mutableListOf<String>()
    private val classesToRegister = mutableSetOf<Class<*>>()
    private val stubDefinitions = mutableListOf<StubsDefinition>()
    private var autoDiscoveryOfStubDefinitions = true
    private val ignoredResources = mutableSetOf<Class<*>>()
    private val enabledByDefaultGroups = mutableListOf<String>()
    private var dependencyRegistryReadyCallback: (DependenciesRegistry) -> Unit = {}

    /**
     * Specifies the TCP port to run the server on it.
     * Default value is 8080.
     * @param port - the TCP port.
     */
    fun onPort(port: Int): StubServer = this.apply {
        this.port = port
    }

    /**
     * Specifies the package to be scanned by Jersey for JAX-RS providers.
     * @param packageName - package name to be scanned.
     */
    fun addPackageToScanForProviders(packageName: String): StubServer = this.apply {
        packagesToScan.add(packageName)
    }

    /**
     * Specifies JAX-RS provider class to be used by Jersey.
     * @param clazz - JAX-RS provider class.
     */
    fun addProviderClassToRegister(clazz: Class<*>): StubServer = this.apply {
        classesToRegister.add(clazz)
    }

    /**
     * Explicitly add StubsDefinition. Makes sense only when [withDisabledAutoDiscoveryOfStubDefinition] is invoked
     * or when this StubsDefinition does not have constructor with no arguments.
     * @param stubDefinition - stubDefinition to be used.
     */
    fun addStubDefinition(stubDefinition: StubsDefinition): StubServer = this.apply {
        stubDefinitions.add(stubDefinition)
    }

    /**
     * Disable auto discovery of [StubsDefinition].
     * Use [addStubDefinition] to add them manually.
     */
    fun withDisabledAutoDiscoveryOfStubDefinition(): StubServer = this.apply {
        autoDiscoveryOfStubDefinitions = false
    }

    /**
     * Specifies the group to be enabled right after the start of the server.
     * Might be useful when need to decide what groups are enabled based on something like environment variables.
     * @param groupName - group to be enabled.
     */
    fun addGroupToEnableOnStart(groupName: String): StubServer = this.apply {
        enabledByDefaultGroups.add(groupName)
    }

    /**
     * Specifies the interface annotated with [jakarta.ws.rs.Path] to be not registered in Jersey.
     * Useful when there are clashes like 'A resource model has ambiguous (sub-)resource method for HTTP method ...'
     * and one of this resources is not needed to be mocked.
     * If all clashing resources should be mocked, consider moving them on different context paths with [withContextPathConfiguration] method.
     * @param ignoredResource - resource class to be ignored.
     */
    fun addResourceToIgnore(ignoredResource: KClass<*>): StubServer = this.apply {
        ignoredResources.add(ignoredResource.java)
    }

    /**
     * Specifies the interface annotated with [jakarta.ws.rs.Path] to be not registered in Jersey
     * Useful when there are clashes like 'A resource model has ambiguous (sub-)resource method for HTTP method ...'
     * and one of this resources is not needed to be mocked.
     * If all clashing resources should be mocked, consider moving them on different context paths with [withContextPathConfiguration] method.
     * @param ignoredResource - resource class to be ignored.
     */
    fun addResourceToIgnore(ignoredResource: Class<*>): StubServer = this.apply {
        ignoredResources.add(ignoredResource)
    }

    /**
     * Provides configuration class to resolve context paths of resource interfaces.
     * By default all resource are resolve to root context path '/'.
     * The configuration is used only once for each resource interface on the start of the server.
     * Use [ByPackageContextPathConfiguration] as ready to use configuration implementation. Or implement your own.
     * @param contextPathsConfiguration - configuration class to resolve context paths.
     */
    fun withContextPathConfiguration(contextPathsConfiguration: ContextPathsConfiguration): StubServer = this.apply {
        this.contextPathsConfiguration = contextPathsConfiguration
    }

    /**
     * Provides configuration class that makes decision whether request should be proxied to external system, and whether request/response
     * should be recorder for further replay.
     * Default implementation is [ForwardWhenNothingMatchedProxyConfiguration].
     * This configuration can work together with proxy/record methods from mocks api. Or it can override them.
     * @param - proxyConfiguration configuration class to make decisions on proxy/record functionality.
     */
    fun withProxyConfiguration(proxyConfiguration: ProxyConfiguration): StubServer = this.also {
        JerseyDependenciesRegistry.proxyConfiguration = proxyConfiguration
    }

    /**
     * Provides HTTP content decoder for recorder.
     * When proxying request to some external system, response might be encoded in some way.
     * To be able to record it as a text, it should be decoded.
     * This method allows to specify such decoders.
     * @param decoderHttp - decoder of HTTP response body.
     */
    fun addHttpResponseDecoder(decoderHttp: HttpResponseDecoder): StubServer = this.also {
        JerseyDependenciesRegistry.httpResponseDecoders += decoderHttp
    }

    /**
     * Specifies the way to save records.
     * By default records are written into console output.
     * Other implementations might for example, write records into some files or send them to external storage.
     * @param saver - saves mock records.
     */
    fun withRecordsSaver(saver: RecordSaver): StubServer = this.also {
        JerseyDependenciesRegistry.recordSaver = saver
    }

    /**
     * Specifies the way to serialize/deserialize mocks.
     * Default implementation is [YamlObjectMapper] which can process mocks in yaml format.
     * @param objectMapper - serialize/deserialize mocks.
     */
    fun withSerializableObjectMapper(objectMapper: SerializableObjectMapper): StubServer = this.also {
        JerseyDependenciesRegistry.serializableMocksObjectMapper = objectMapper
    }

    /**
     * Specifies the way to load serialized mocks.
     * Default implementation is [ResourceFolderSerialisedMocksLoader] configured to load yaml files from resource folder of application.
     * Other implementation might for example, read mock from other folders or external storage.
     * @param loader - loads serialized mocks.
     */
    fun withSerialisedMocksLoader(loader: SerialisedMocksLoader): StubServer = this.also {
        JerseyDependenciesRegistry.serialisedMocksLoader = loader
    }

    /**
     * Specifies the way to deserialize and load objects which are returned by Resources.
     * Default implementation are
     * [JacksonResponseBodyProvider] - deserialize json string into objects with Jackson.
     * [JerseyInternalResponseBodyProvider] - deserialize json string into objects with internal jersey
     * mechanism. Uses registered JAX-RS providers. Use [JerseyDependenciesRegistry.jerseyInternalBodyProvider] to get it.
     * [ResourceFolderFilesResponseBodyProvider] - load matching file from resource folder and use other provider to deserialize it.
     * Other implementations might for example, read response files from other folders or external storage or deserialize other formats.
     * @param responseBodyProvider - provides response body from string.
     */
    fun withDefaultResponseBodyProvider(responseBodyProvider: ResponseBodyProvider): StubServer = this.also {
        JerseyDependenciesRegistry.defaultResponseBodyProvider = responseBodyProvider
    }

    /**
     * The legit way to get [DependenciesRegistry]
     * There are some useful classes constructed by this application like [HttpRequestResponseHolder] or [SerialisationUtils].
     * Use this method to get the registry with this dependencies.
     * Alternatively [JerseyDependenciesRegistry] object can be used directly. But it should be used only after the start method of the server
     * is invoked.
     * @param callback to be invoked whe DependenciesRegistry is fully constructed.
     */
    fun onDependenciesRegistryReady(callback: (DependenciesRegistry) -> Unit): StubServer = this.apply {
       dependencyRegistryReadyCallback = callback
    }

    /**
     * Starts the server.
     * The thread invoking this method will be blocked infinitely.
     */
    fun start() {
        logger.info("Starting the server")
        val tomcat = Tomcat()
        tomcat.setPort(port)
        tomcat.getConnector()
        addStubDefinition(GroupConfigurationResourceImpl())
        addStubDefinition(JerseyDependenciesRegistry.serializableMocksLoadingStubsDefinition)
        JerseyDependenciesRegistry.stubDefinitions = getStubDefinitions()
        enabledByDefaultGroups.forEach { JerseyDependenciesRegistry.groupSwitchService().switchGroupStatus(it, GroupStatus.ACTIVE) }
        getResourceInterfacesToContextMapping(ignoredResources).forEach { contextPath, interfaces ->
            logger.info("adding context {}", contextPath)
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
        context.addServletMappingDecoded("/*", "jersey-container-servlet")
    }

    private fun registerResources(resourceLoader: ResourceConfig, interfacesToMock: List<Class<*>>) {
        val proxyFactor = JerseyDependenciesRegistry.proxyInstanceFactory()
        val proxyInstances = interfacesToMock.map { resource -> proxyFactor.createMockInstanceForInterface(resource) }
        resourceLoader.registerInstances(proxyInstances.toSet())
    }

    private fun getResourceInterfacesToContextMapping(ignoredResources: Set<Class<*>>): Map<String, List<Class<*>>> {
        return AutoDiscoveryOfResourceInterfaces(ignoredResources).getInterfacesToMock()
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
            return stubDefinitions + AutoDiscoveryOfStubDefinitions().getStubDefinitions()
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
