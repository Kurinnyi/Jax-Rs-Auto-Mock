package ua.kurinnyi.jaxrs.auto.mock.jersey

import ua.kurinnyi.jaxrs.auto.mock.DependenciesRegistry
import ua.kurinnyi.jaxrs.auto.mock.GroupSwitchService
import ua.kurinnyi.jaxrs.auto.mock.MethodInvocationHandler
import ua.kurinnyi.jaxrs.auto.mock.ProxyInstanceFactory
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.FileBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.JacksonBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.TemplateEngine
import ua.kurinnyi.jaxrs.auto.mock.filters.ContextSaveFilter
import ua.kurinnyi.jaxrs.auto.mock.filters.ResponseIntersectingFilter
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.NothingMatchedProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.RequestProxy
import ua.kurinnyi.jaxrs.auto.mock.mocks.SerialisationUtils
import ua.kurinnyi.jaxrs.auto.mock.mocks.MethodStubsLoader
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.recorder.ConsoleRecordSaver
import ua.kurinnyi.jaxrs.auto.mock.recorder.RecordSaver
import ua.kurinnyi.jaxrs.auto.mock.recorder.Recorder
import ua.kurinnyi.jaxrs.auto.mock.recorder.ResponseDecoder
import ua.kurinnyi.jaxrs.auto.mock.yaml.ResourceFolderYamlFilesLoader
import ua.kurinnyi.jaxrs.auto.mock.yaml.YamlFilesLoader

object JerseyDependenciesRegistry : DependenciesRegistry {

    override fun recorder() = recorder
    override fun groupSwitchService() = commonDependencies.groupSwitchService
    override fun serialisationUtils():SerialisationUtils = SERIALISATION_UTILS
    override fun requestProxy():RequestProxy = requestProxy
    override fun contextSaveFilter():ContextSaveFilter = contextSaveFilter
    override fun bodyProvider():BodyProvider = defaultBodyProvider
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
    var defaultBodyProvider: BodyProvider = FileBodyProvider(jacksonBodyProvider)

    private val recorder: Recorder by lazy {
        Recorder(responseDecoders, recordSaver, contextSaveFilter, responseIntersectingFilter, platformUtils)
    }
    private val commonDependencies: CommonDependencies by lazy {
        CommonDependencies(proxyConfiguration, stubDefinitions, this)
    }
    private val SERIALISATION_UTILS: SerialisationUtils by lazy {
        SerialisationUtils(defaultBodyProvider, TemplateEngine())
    }

    data class CommonDependencies(val proxyConfiguration: ProxyConfiguration,
                                  val stubDefinitions: List<StubsDefinition>,
                                  val dependenciesRegistry: DependenciesRegistry) {
        private val methodStubsLoader = MethodStubsLoader(stubDefinitions, proxyConfiguration)
        val groupSwitchService = GroupSwitchService(methodStubsLoader)
        val proxyInstanceFactory =
                ProxyInstanceFactory(MethodInvocationHandler(methodStubsLoader, proxyConfiguration, dependenciesRegistry))
    }
}