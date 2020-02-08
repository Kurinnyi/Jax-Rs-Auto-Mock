package ua.kurinnyi.jaxrs.auto.mock.body

import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.reflect.Type

class JacksonBodyProvider: BodyProvider {
    private val objectMapper = ObjectMapper()

    override fun <T> provideBodyObjectFromJson(type: Class<T>, genericType: Type, bodyJson: String) =
            provideBodyObject(type, genericType, bodyJson)

    override fun provideBodyJson(body: String): String  = body

    override fun <T> provideBodyObject(type:Class<T>, genericType: Type, body: String): T {
        return objectMapper.readValue(body, type)
    }

    override fun <T> objectToJson(value: T, type: Class<T>, genericType: Type): String {
        return objectMapper.writeValueAsString(value)
    }
}