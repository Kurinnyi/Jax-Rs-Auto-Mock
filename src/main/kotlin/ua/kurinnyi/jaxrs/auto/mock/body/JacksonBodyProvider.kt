package ua.kurinnyi.jaxrs.auto.mock.body

import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.reflect.Type

object JacksonBodyProvider: BodyProvider {
    override fun <T> provideBodyObject(type:Class<T>, genericType: Type, body: String): T {
        return ObjectMapper().readValue(body, type)
    }
}