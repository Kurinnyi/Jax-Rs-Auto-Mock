package ua.kurinnyi.jaxrs.auto.mock.httpproxy

import ua.kurinnyi.jaxrs.auto.mock.yaml.YamlObjectMapper

class YamlProvidedNothingMatchedProxyConfig(yamlFile: String) :
        ExternallyProvidedNothingMatchedProxyConfig(
                readYamlProxyConfig(yamlFile),
                readYamlRecordConfig(yamlFile)) {

    companion object {
        fun readYamlProxyConfig(yamlFile: String): Map<String, String> =
            readYamlConfig(yamlFile).config.map {
                it.javaPath to it.destination
            }.toMap()

        fun readYamlRecordConfig(yamlFile: String): Set<String> =
            readYamlConfig(yamlFile).config
                    .filter { it.record == true }
                    .map { it.javaPath }
                    .toSet()


        private fun readYamlConfig(yamlFile: String): YamlConfig {
            val yamlStream = this::class.java.getResourceAsStream(yamlFile)
                    ?: throw IllegalArgumentException("Yaml file $yamlFile not found")
            return yamlStream.use { YamlObjectMapper.read(it) }
        }
    }

    data class YamlConfig(val config: List<YamlConfigEntry>)

    data class YamlConfigEntry (
            val javaPath:String,
            val destination:String,
            val record:Boolean?)
}


