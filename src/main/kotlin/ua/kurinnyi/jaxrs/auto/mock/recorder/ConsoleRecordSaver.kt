package ua.kurinnyi.jaxrs.auto.mock.recorder

import ua.kurinnyi.jaxrs.auto.mock.yaml.MethodStubsHolder

object ConsoleRecordSaver : RecordSaver {
    override fun saveRecords(stubs: MethodStubsHolder) {
        println(toYamlString(stubs))
    }
}