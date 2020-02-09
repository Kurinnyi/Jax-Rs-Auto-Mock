package ua.kurinnyi.jaxrs.auto.mock.jersey

import ua.kurinnyi.jaxrs.auto.mock.extensions.ResponseBodyProvider
import java.lang.reflect.Type

class JerseyInternalResponseBodyProvider(private val jerseyInternalsFilter: JerseyInternalsFilter): ResponseBodyProvider {
    override fun <T> provideBodyObjectFromString(type: Class<T>, genericType: Type, bodyString: String) =
            jerseyInternalsFilter.prepareResponse(type, genericType, bodyString)

    override fun provideBodyString(bodyInformation: String): String  = bodyInformation

    override fun <T> objectToString(value: T, type: Class<T>, genericType: Type): String {
        return jerseyInternalsFilter.toJson(value, type, genericType)
    }
}