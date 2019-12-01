package ua.kurinnyi.jaxrs.auto.mock.recorder

import ua.kurinnyi.jaxrs.auto.mock.yaml.MethodStubsHolder
import ua.kurinnyi.jaxrs.auto.mock.yaml.YamlObjectMapper

interface RecordSaver {
    fun saveRecords(stubs: MethodStubsHolder)

    fun toYamlString(stubs:MethodStubsHolder):String =
        YamlObjectMapper.toString(stubs)

}