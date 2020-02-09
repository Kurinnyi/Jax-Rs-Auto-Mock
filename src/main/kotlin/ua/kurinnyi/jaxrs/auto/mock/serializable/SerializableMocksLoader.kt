package ua.kurinnyi.jaxrs.auto.mock.serializable

import ua.kurinnyi.jaxrs.auto.mock.extensions.SerialisedMocksFilesLoader
import ua.kurinnyi.jaxrs.auto.mock.extensions.SerializableObjectMapper
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.MethodMock
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.CompleteMocksData


class SerializableMocksLoader(
        private val mocksFilesLoader: SerialisedMocksFilesLoader,
        private val serializableObjectMapper: SerializableObjectMapper,
        private val serializableMockToExecutableMethodMockConverter: SerializableMockToExecutableMethodMockConverter
) : StubsDefinition {

    override fun getPriority(): Int = 1
    override fun isHotReloadable(): Boolean = true
    override fun getGroupsCallbacks(): List<GroupCallback> = emptyList()

    override fun getStubs(): CompleteMocksData {
        return CompleteMocksData(methodMocks = loadMethodMocks())
    }

    private fun loadMethodMocks(): List<MethodMock> =
            mocksFilesLoader.reloadFilesAsStrings()
                    .map { serializableObjectMapper.read(it, SerializableMocksHolder::class.java).stubs }
                    .flatten()
                    .flatMap { serializableMockToExecutableMethodMockConverter.toExecutableMock(it) }
}
