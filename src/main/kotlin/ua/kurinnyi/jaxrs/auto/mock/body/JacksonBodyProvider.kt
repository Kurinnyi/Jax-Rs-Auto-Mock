package ua.kurinnyi.jaxrs.auto.mock.body

import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.reflect.Type

class JacksonBodyProvider: BodyProvider {
    private val objectMapper = ObjectMapper()

    override fun <T> provideBodyObjectFromJson(type: Class<T>, genericType: Type, bodyJson: String):T =
            objectMapper.readValue(bodyJson, type)

    override fun provideBodyJson(body: String): String  = body

    override fun <T> objectToJson(value: T, type: Class<T>, genericType: Type): String =
        objectMapper.writeValueAsString(value)
}