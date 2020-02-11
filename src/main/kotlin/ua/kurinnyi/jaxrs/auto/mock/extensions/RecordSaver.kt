package ua.kurinnyi.jaxrs.auto.mock.extensions

import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMocksHolder

interface RecordSaver {
    fun saveRecords(mocks: SerializableMocksHolder, objectMapper: SerializableObjectMapper)
}