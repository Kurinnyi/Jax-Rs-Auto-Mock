package ua.kurinnyi.jaxrs.auto.mock.extensions

import ua.kurinnyi.jaxrs.auto.mock.extensions.defaul.YamlObjectMapper
/**
 * The interface to serialize and deserialize mocks.
 * Default implementation is [YamlObjectMapper] which works with files formatted as Yaml
 */
interface SerializableObjectMapper {

    /**
     * This method converts provided string content into the required class.
     * @param content - content to be deserialized.
     * @param clazz - the required type to get after deserialization.
     * @return - deserialized object.
     */
    fun <T> read(content: String, clazz:Class<T>):T

    /**
     * This method serialize objects to string.
     * @param value - object to be serialized to string
     * @return serialized object as string
     */
    fun <T> toString(value: T):String

}