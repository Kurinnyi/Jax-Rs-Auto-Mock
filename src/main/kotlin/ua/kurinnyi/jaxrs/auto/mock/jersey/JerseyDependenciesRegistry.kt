package ua.kurinnyi.jaxrs.auto.mock.jersey

import ua.kurinnyi.jaxrs.auto.mock.DependenciesRegistry
import ua.kurinnyi.jaxrs.auto.mock.GroupSwitchService
import ua.kurinnyi.jaxrs.auto.mock.MethodInvocationHandler
import ua.kurinnyi.jaxrs.auto.mock.ProxyInstanceFactory
import ua.kurinnyi.jaxrs.auto.mock.body.ResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.FromFileResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.JacksonResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.TemplateEngine
import ua.kurinnyi.jaxrs.auto.mock.filters.HttpRequestResponseHolder
import ua.kurinnyi.jaxrs.auto.mock.filters.ResponseIntersectingFilter
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ForwardWhenNothingMatchedProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.RequestProxy
import ua.kurinnyi.jaxrs.auto.mock.mocks.SerialisationUtils
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubDefinitionsExecutor
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.recorder.ConsoleRecordSaver
import ua.kurinnyi.jaxrs.auto.mock.recorder.RecordSaver
import ua.kurinnyi.jaxrs.auto.mock.recorder.Recorder
import ua.kurinnyi.jaxrs.auto.mock.recorder.HttpResponseDecoder
import ua.kurinnyi.jaxrs.auto.mock.serializable.*

object JerseyDependenciesRegistry : DependenciesRegistry {

    override fun recorder() = recorder
    override fun groupSwitchService() = commonDependencies.groupSwitchService
    override fun serialisationUtils():SerialisationUtils = serialisationUtils
    override fun requestProxy():RequestProxy = requestProxy
    override fun httpRequestResponseHolder():HttpRequestResponseHolder = contextSaveFilter
    override fun responseBodyProvider():ResponseBodyProvider = defaultResponseBodyProvider
    override fun platformUtils() = platformUtils

    fun proxyInstanceFactory() = commonDependencies.proxyInstanceFactory

    val responseIntersectingFilter = ResponseIntersectingFilter()
    val jerseyInternalsFilter = JerseyInternalsFilter()
    val jerseyInternalBodyProvider = JerseyInternalResponseBodyProvider(jerseyInternalsFilter)
    val jacksonBodyProvider = JacksonResponseBodyProvider()
    private val contextSaveFilter = HttpRequestResponseHolder()
    private val requestProxy = RequestProxy(contextSaveFilter)
    private val platformUtils = JerseyPlatformUtils()
    private val yamlObjectMapper = YamlObjectMapper()

    var recordSaver: RecordSaver = ConsoleRecordSaver()
    var httpResponseDecoders: List<HttpResponseDecoder> = listOf()
    var proxyConfiguration: ProxyConfiguration = ForwardWhenNothingMatchedProxyConfiguration()
    var serialisedMocksFilesLoader: SerialisedMocksFilesLoader = ResourceFolderSerialisedMocksFilesLoader(".yaml")
    var stubDefinitions: List<StubsDefinition> = emptyList()
    var defaultResponseBodyProvider: ResponseBodyProvider = FromFileResponseBodyProvider(jacksonBodyProvider)

    private val recorder: Recorder by lazy {
        Recorder(httpResponseDecoders, recordSaver, contextSaveFilter, responseIntersectingFilter, platformUtils, yamlObjectMapper)
    }
    private val commonDependencies: CommonDependencies by lazy {
        CommonDependencies(proxyConfiguration, stubDefinitions, this)
    }
    private val serialisationUtils: SerialisationUtils by lazy {
        SerialisationUtils(defaultResponseBodyProvider, TemplateEngine())
    }
    val serializableMocksLoader: SerializableMocksLoader by lazy {
        SerializableMocksLoader(serialisedMocksFilesLoader, yamlObjectMapper, SerializableMockToExecutableMethodMockConverter())
    }

    data class CommonDependencies(val proxyConfiguration: ProxyConfiguration,
                                  val stubDefinitions: List<StubsDefinition>,
                                  val dependenciesRegistry: DependenciesRegistry) {
        private val stubDefinitionsExecutor = StubDefinitionsExecutor(stubDefinitions, proxyConfiguration)
        val groupSwitchService = GroupSwitchService(stubDefinitionsExecutor)
        val proxyInstanceFactory =
                ProxyInstanceFactory(MethodInvocationHandler(stubDefinitionsExecutor, proxyConfiguration, dependenciesRegistry))
    }
}