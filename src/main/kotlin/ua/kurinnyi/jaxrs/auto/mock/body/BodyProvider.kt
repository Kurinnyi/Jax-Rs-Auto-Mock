package ua.kurinnyi.jaxrs.auto.mock.body

import java.lang.reflect.Type

interface BodyProvider {
    fun <T> provideBodyObject(type: Class<T>, genericType: Type, body: String): T
    fun <T> provideBodyObjectFromJson(type: Class<T>, genericType: Type, bodyJson: String): T
    fun provideBodyJson(body: String): String
    fun <T> objectToJson(value: T, type:Class<T>, genericType: Type): String
}