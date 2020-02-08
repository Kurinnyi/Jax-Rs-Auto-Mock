package ua.kurinnyi.jaxrs.auto.mock.body

import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.reflect.Type

class JacksonBodyProvider: BodyProvider {
    private val objectMapper = ObjectMapper()

    override fun <T> provideBodyObjectFromString(type: Class<T>, genericType: Type, bodyString: String):T =
            objectMapper.readValue(bodyString, type)

    override fun provideBodyString(bodyInformation: String): String  = bodyInformation

    override fun <T> objectToString(value: T, type: Class<T>, genericType: Type): String =
        objectMapper.writeValueAsString(value)
}