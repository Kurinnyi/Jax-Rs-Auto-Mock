package ua.kurinnyi.jaxrs.auto.mock.recorder

import ua.kurinnyi.jaxrs.auto.mock.serializable.MethodStubsHolder
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableObjectMapper

interface RecordSaver {
    fun saveRecords(stubs: MethodStubsHolder, objectMapper: SerializableObjectMapper)
}