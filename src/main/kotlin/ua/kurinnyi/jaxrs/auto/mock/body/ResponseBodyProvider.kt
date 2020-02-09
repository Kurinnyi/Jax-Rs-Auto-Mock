package ua.kurinnyi.jaxrs.auto.mock.body

import java.lang.reflect.Type

interface ResponseBodyProvider {
    fun <T> provideBodyObjectFromString(type: Class<T>, genericType: Type, bodyString: String): T
    fun provideBodyString(bodyInformation: String): String
    fun <T> objectToString(value: T, type:Class<T>, genericType: Type): String
}