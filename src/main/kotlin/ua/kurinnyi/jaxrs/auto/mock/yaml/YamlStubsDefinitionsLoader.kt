package ua.kurinnyi.jaxrs.auto.mock.yaml

import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.StubDefinitionData


class YamlStubsDefinitionsLoader(private val filesLoader: YamlFilesLoader) : StubsDefinition {

    override fun getPriority(): Int = 1
    override fun isRealTime(): Boolean = true
    override fun getGroupsCallbacks(): List<GroupCallback> = emptyList()

    override fun getStubs(): StubDefinitionData {
        return StubDefinitionData(methodStubs = getMethodStubResponses())
    }

    private fun getMethodStubResponses(): List<ResourceMethodStub> =
            filesLoader.reloadYamlFilesAsStrings()
                    .map { YamlObjectMapper.read<MethodStubsHolder>(it).stubs }
                    .flatten()
                    .flatMap { it.toFlatStubs() }
}
