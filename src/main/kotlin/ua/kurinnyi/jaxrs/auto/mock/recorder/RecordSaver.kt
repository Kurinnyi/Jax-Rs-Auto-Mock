package ua.kurinnyi.jaxrs.auto.mock.recorder

import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMocksHolder
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableObjectMapper

interface RecordSaver {
    fun saveRecords(mocks: SerializableMocksHolder, objectMapper: SerializableObjectMapper)
}