package ua.kurinnyi.jaxrs.auto.mock.body

import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.reflect.Type

object JacksonBodyProvider: BodyProvider {
    override fun <T> provideBodyObjectFromJson(type: Class<T>, genericType: Type, bodyJson: String) =
            provideBodyObject(type, genericType, bodyJson)

    override fun provideBodyJson(body: String): String  = body

    override fun <T> provideBodyObject(type:Class<T>, genericType: Type, body: String): T {
        return ObjectMapper().readValue(body, type)
    }
}