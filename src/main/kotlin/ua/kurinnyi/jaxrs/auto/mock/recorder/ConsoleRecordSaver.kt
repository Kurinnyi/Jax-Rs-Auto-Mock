package ua.kurinnyi.jaxrs.auto.mock.recorder

import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMocksHolder
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableObjectMapper

class ConsoleRecordSaver : RecordSaver {
    override fun saveRecords(mocks: SerializableMocksHolder, objectMapper: SerializableObjectMapper) {
        println(objectMapper.toString(mocks))
    }
}