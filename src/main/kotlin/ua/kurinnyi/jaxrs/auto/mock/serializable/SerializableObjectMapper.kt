package ua.kurinnyi.jaxrs.auto.mock.serializable

interface SerializableObjectMapper {

    fun <T> read(content: String, clazz:Class<T>):T

    fun <T> toString(value: T):String

}