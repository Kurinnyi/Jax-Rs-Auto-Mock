package ua.kurinnyi.jaxrs.auto.mock.body

import ua.kurinnyi.jaxrs.auto.mock.JerseyInternalsFilter
import java.lang.reflect.Type

object JerseyInternalBodyProvider: BodyProvider {

    override fun <T> provideBodyObjectFromJson(type: Class<T>, genericType: Type, bodyJson: String) =
            JerseyInternalBodyProvider.provideBodyObject(type, genericType, bodyJson)

    override fun provideBodyJson(body: String): String  = body

    override fun <T> provideBodyObject(type:Class<T>, genericType: Type, body: String): T {
        return JerseyInternalsFilter.prepareResponse(type, genericType, body)
    }
}