package ua.kurinnyi.jaxrs.auto.mock.yaml

import ua.kurinnyi.jaxrs.auto.mock.MethodStubsLoader
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.StubsGroup
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class YamlMethodStubsLoader(private val filesLoader: YamlFilesLoader) : MethodStubsLoader {

    override fun getGroupsCallbacks(): List<GroupCallback> = emptyList()
    override fun getGroups(): List<StubsGroup> = emptyList()

    private var stubs: List<ResourceMethodStub>

    init {
        stubs = getMethodStubResponses()
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate({ stubs = getMethodStubResponses() }, 0, 10, TimeUnit.SECONDS)
    }

    override fun getStubs(): List<ResourceMethodStub> {
        return stubs
    }

    private fun getMethodStubResponses(): List<ResourceMethodStub> =
            filesLoader.reloadYamlFilesAsStrings()
                    .map { YamlObjectMapper.read<MethodStubsHolder>(it).stubs }
                    .flatten()
                    .flatMap { it.toFlatStubs() }
}
