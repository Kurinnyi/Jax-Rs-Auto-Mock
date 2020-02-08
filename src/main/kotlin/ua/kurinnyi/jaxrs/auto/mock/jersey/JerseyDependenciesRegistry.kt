package ua.kurinnyi.jaxrs.auto.mock.jersey

import ua.kurinnyi.jaxrs.auto.mock.*
import ua.kurinnyi.jaxrs.auto.mock.body.*
import ua.kurinnyi.jaxrs.auto.mock.filters.ContextSaveFilter
import ua.kurinnyi.jaxrs.auto.mock.filters.ResponseIntersectingFilter
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.NothingMatchedProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.RequestProxy
import ua.kurinnyi.jaxrs.auto.mock.mocks.JsonUtils
import ua.kurinnyi.jaxrs.auto.mock.mocks.KotlinMethodStubsLoader
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.recorder.ConsoleRecordSaver
import ua.kurinnyi.jaxrs.auto.mock.recorder.RecordSaver
import ua.kurinnyi.jaxrs.auto.mock.recorder.Recorder
import ua.kurinnyi.jaxrs.auto.mock.recorder.ResponseDecoder
import ua.kurinnyi.jaxrs.auto.mock.yaml.ResourceFolderYamlFilesLoader
import ua.kurinnyi.jaxrs.auto.mock.yaml.YamlFilesLoader
import ua.kurinnyi.jaxrs.auto.mock.yaml.YamlMethodStubsLoader

object JerseyDependenciesRegistry : DependenciesRegistry {

    override fun recorder() = recorder
    override fun groupSwitchService() = commonDependencies.groupSwitchService
    override fun jsonUtils():JsonUtils = jsonUtils
    override fun requestProxy():RequestProxy = requestProxy
    override fun contextSaveFilter():ContextSaveFilter = contextSaveFilter
    override fun bodyProvider():BodyProvider = defaultBodyProvider
    override fun extractingBodyProvider():ExtractingBodyProvider = defaultExtractingBodyProvider
    override fun platformUtils() = platformUtils

    fun proxyInstanceFactory() = commonDependencies.proxyInstanceFactory

    val responseIntersectingFilter = ResponseIntersectingFilter()
    val jerseyInternalsFilter = JerseyInternalsFilter()
    val jerseyInternalBodyProvider = JerseyInternalBodyProvider(jerseyInternalsFilter)
    val jacksonBodyProvider = JacksonBodyProvider()
    private val contextSaveFilter = ContextSaveFilter()
    private val requestProxy = RequestProxy(contextSaveFilter)
    private val platformUtils = JerseyPlatformUtils()

    var recordSaver: RecordSaver = ConsoleRecordSaver()
    var responseDecoders: List<ResponseDecoder> = listOf()
    var proxyConfiguration: ProxyConfiguration = NothingMatchedProxyConfiguration()
    var yamlFilesLoader: YamlFilesLoader = ResourceFolderYamlFilesLoader
    var stubDefinitions: List<StubsDefinition> = emptyList()
    var defaultBodyProvider: BodyProvider = jacksonBodyProvider
    var defaultExtractingBodyProvider:ExtractingBodyProvider = FileBodyProvider(defaultBodyProvider)

    private val recorder: Recorder by lazy {
        Recorder(responseDecoders, recordSaver, contextSaveFilter, responseIntersectingFilter, platformUtils)
    }
    private val commonDependencies: CommonDependencies by lazy {
        CommonDependencies(yamlFilesLoader, proxyConfiguration, stubDefinitions, this)
    }
    private val jsonUtils: JsonUtils by lazy {
        JsonUtils(defaultBodyProvider, defaultExtractingBodyProvider, jerseyInternalsFilter, TemplateEngine())
    }

    data class CommonDependencies(val yamlFilesLoader: YamlFilesLoader,
                                  val proxyConfiguration: ProxyConfiguration,
                                  val stubDefinitions: List<StubsDefinition>,
                                  val dependenciesRegistry: DependenciesRegistry) {
        private val methodStubsLoader = CompositeMethodStubLoader(
                KotlinMethodStubsLoader(stubDefinitions, proxyConfiguration), YamlMethodStubsLoader(yamlFilesLoader))
        val groupSwitchService = GroupSwitchService(methodStubsLoader)
        val proxyInstanceFactory =
                ProxyInstanceFactory(MethodInvocationHandler(methodStubsLoader, proxyConfiguration, dependenciesRegistry))
    }
}