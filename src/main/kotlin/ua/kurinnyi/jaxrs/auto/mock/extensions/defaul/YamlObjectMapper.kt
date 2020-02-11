package ua.kurinnyi.jaxrs.auto.mock.extensions.defaul

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import ua.kurinnyi.jaxrs.auto.mock.extensions.SerializableObjectMapper

class YamlObjectMapper : SerializableObjectMapper {
    private val yamlObjectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    override fun <T> read(content: String, clazz:Class<T>):T = yamlObjectMapper.readValue(content, clazz)

    override fun <T> toString(value: T):String = yamlObjectMapper.writeValueAsString(value)
}