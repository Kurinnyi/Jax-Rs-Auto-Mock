package ua.kurinnyi.jaxrs.auto.mock.jersey

import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import java.lang.reflect.Type

class JerseyInternalBodyProvider(private val jerseyInternalsFilter: JerseyInternalsFilter): BodyProvider {
    override fun <T> provideBodyObjectFromJson(type: Class<T>, genericType: Type, bodyJson: String) =
            provideBodyObject(type, genericType, bodyJson)

    override fun provideBodyJson(body: String): String  = body

    override fun <T> provideBodyObject(type:Class<T>, genericType: Type, body: String): T {
        return jerseyInternalsFilter.prepareResponse(type, genericType, body)
    }

    override fun <T> objectToJson(value: T, type: Class<T>, genericType: Type): String {
        return jerseyInternalsFilter.toJson(value, type, genericType)
    }
}