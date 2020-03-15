package ua.kurinnyi.jaxrs.auto.mock.extensions.defaul

import ua.kurinnyi.jaxrs.auto.mock.extensions.RecordSaver
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMocksHolder
import ua.kurinnyi.jaxrs.auto.mock.extensions.SerializableObjectMapper

/**
 * Default implementation of [RecordSaver].
 * It writes records to the console.
 */
class ConsoleRecordSaver : RecordSaver {
    override fun saveRecords(mocks: SerializableMocksHolder, objectMapper: SerializableObjectMapper) {
        println(objectMapper.toString(mocks))
    }
}