package ua.kurinnyi.jaxrs.auto.mock.body

import java.lang.reflect.Type

interface BodyProvider {
    fun <T> provideBodyObject(type: Class<T>, genericType: Type, body: String): T
}