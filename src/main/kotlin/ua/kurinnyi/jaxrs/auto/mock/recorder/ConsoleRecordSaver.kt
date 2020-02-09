package ua.kurinnyi.jaxrs.auto.mock.recorder

import ua.kurinnyi.jaxrs.auto.mock.serializable.MethodStubsHolder
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableObjectMapper

class ConsoleRecordSaver : RecordSaver {
    override fun saveRecords(stubs: MethodStubsHolder, objectMapper: SerializableObjectMapper) {
        println(objectMapper.toString(stubs))
    }
}