package ua.kurinnyi.jaxrs.auto.mock.extensions.defaul

import com.fasterxml.jackson.databind.ObjectMapper
import ua.kurinnyi.jaxrs.auto.mock.extensions.ResponseBodyProvider
import java.lang.reflect.Type

/**
 * This implementation uses provided body as string in Json format and deserialize it with Jackson
 * It does not load the content, and only process it.
 */
class JacksonResponseBodyProvider: ResponseBodyProvider {
    private val objectMapper = ObjectMapper()

    override fun <T> provideBodyObjectFromString(type: Class<T>, genericType: Type, bodyString: String):T =
            objectMapper.readValue(bodyString, type)

    override fun provideBodyString(bodyInformation: String): String  = bodyInformation

    override fun <T> objectToString(value: T, type: Class<T>, genericType: Type): String =
        objectMapper.writeValueAsString(value)
}