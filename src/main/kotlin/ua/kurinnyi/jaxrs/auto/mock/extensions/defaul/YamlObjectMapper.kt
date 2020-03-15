package ua.kurinnyi.jaxrs.auto.mock.extensions.defaul

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import ua.kurinnyi.jaxrs.auto.mock.extensions.SerializableObjectMapper


/**
 * This class is used to serialize/deserialize mocks in Yaml format.
 * It use Jackson under the hood.
 */
class YamlObjectMapper : SerializableObjectMapper {
    private val yamlObjectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    override fun <T> read(content: String, clazz:Class<T>):T = yamlObjectMapper.readValue(content, clazz)

    override fun <T> toString(value: T):String = yamlObjectMapper.writeValueAsString(value)
}