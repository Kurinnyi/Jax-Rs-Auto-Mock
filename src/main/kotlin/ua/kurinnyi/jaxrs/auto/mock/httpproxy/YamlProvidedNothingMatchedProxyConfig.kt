package ua.kurinnyi.jaxrs.auto.mock.httpproxy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule

class YamlProvidedNothingMatchedProxyConfig(yamlFile:String): ExternallyProvidedNothingMatchedProxyConfig(readYaml(yamlFile)){

    companion object {
        fun readYaml(yamlFile:String):Map<String, String> {
            val yamlObjectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())
            val yamlStream = this::class.java.getResourceAsStream(yamlFile) ?: throw IllegalArgumentException("Yaml file $yamlFile not found")
            return yamlStream.use {
                yamlObjectMapper.readValue(it, Map::class.java) as Map<String, String>
            }
        }
    }

}
