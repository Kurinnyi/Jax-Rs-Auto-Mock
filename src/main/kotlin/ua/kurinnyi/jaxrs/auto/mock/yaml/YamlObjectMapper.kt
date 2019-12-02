package ua.kurinnyi.jaxrs.auto.mock.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.BufferedReader
import java.io.InputStream

object YamlObjectMapper {
    val _yamlObjectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    inline fun <reified T> read(reader: InputStream):T =
        _yamlObjectMapper.readValue(reader, T::class.java)

    inline fun <reified T> read(content: String):T =
        _yamlObjectMapper.readValue(content, T::class.java)

    fun <T> toString(value: T):String =
            _yamlObjectMapper.writeValueAsString(value)

}