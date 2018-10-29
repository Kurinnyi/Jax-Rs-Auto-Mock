package ua.kurinnyi.jaxrs.auto.mock.body

import ua.kurinnyi.jaxrs.auto.mock.JerseyInternalsFilter
import java.lang.reflect.Type

object JerseyInternalBodyProvider: BodyProvider {
    override fun <T> provideBodyObject(type:Class<T>, genericType: Type, body: String): T {
        return JerseyInternalsFilter.prepareResponse(type, genericType, body)
    }
}