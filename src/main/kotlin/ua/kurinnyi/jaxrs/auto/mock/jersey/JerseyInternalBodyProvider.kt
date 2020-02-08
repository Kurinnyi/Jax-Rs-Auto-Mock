package ua.kurinnyi.jaxrs.auto.mock.jersey

import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import java.lang.reflect.Type

class JerseyInternalBodyProvider(private val jerseyInternalsFilter: JerseyInternalsFilter): BodyProvider {
    override fun <T> provideBodyObjectFromString(type: Class<T>, genericType: Type, bodyString: String) =
            jerseyInternalsFilter.prepareResponse(type, genericType, bodyString)

    override fun provideBodyString(bodyInformation: String): String  = bodyInformation

    override fun <T> objectToString(value: T, type: Class<T>, genericType: Type): String {
        return jerseyInternalsFilter.toJson(value, type, genericType)
    }
}