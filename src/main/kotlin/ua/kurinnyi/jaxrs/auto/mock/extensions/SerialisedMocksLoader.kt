package ua.kurinnyi.jaxrs.auto.mock.extensions

import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMethodMock

interface SerialisedMocksLoader {
    fun reloadMocks(serializableObjectMapper: SerializableObjectMapper):List<SerializableMethodMock>
}