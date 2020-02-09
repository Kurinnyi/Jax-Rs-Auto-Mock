package ua.kurinnyi.jaxrs.auto.mock.serializable

import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.StubDefinitionData


class SerializableStubsDefinitionLoader(
        private val filesLoader: SerializableFilesLoader,
        private val serializableObjectMapper: SerializableObjectMapper,
        private val serializableToMethodStubConverter: SerializableToMethodStubConverter
) : StubsDefinition {

    override fun getPriority(): Int = 1
    override fun isRealTime(): Boolean = true
    override fun getGroupsCallbacks(): List<GroupCallback> = emptyList()

    override fun getStubs(): StubDefinitionData {
        return StubDefinitionData(methodStubs = getMethodStubResponses())
    }

    private fun getMethodStubResponses(): List<ResourceMethodStub> =
            filesLoader.reloadFilesAsStrings()
                    .map { serializableObjectMapper.read(it, MethodStubsHolder::class.java).stubs }
                    .flatten()
                    .flatMap { serializableToMethodStubConverter.toMethodStubs(it) }
}
