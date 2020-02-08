package ua.kurinnyi.jaxrs.auto.mock.recorder

import ua.kurinnyi.jaxrs.auto.mock.yaml.MethodStubsHolder

class ConsoleRecordSaver : RecordSaver {
    override fun saveRecords(stubs: MethodStubsHolder) {
        println(toYamlString(stubs))
    }
}