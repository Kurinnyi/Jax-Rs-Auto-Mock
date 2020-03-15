package ua.kurinnyi.jaxrs.auto.mock.extensions

import ua.kurinnyi.jaxrs.auto.mock.extensions.defaul.JacksonResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.extensions.defaul.ResourceFolderFilesResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.mocks.SerialisationUtils
import java.lang.reflect.Type

/**
 * Specifies the way to deserialize and load objects which are returned by Resources.
 * It is used when methods like [bodyJson] from API or any method from [SerialisationUtils] is invoked.
 * Default implementation are:
 * [JacksonResponseBodyProvider] - deserialize json string into objects with Jackson.
 * [ResourceFolderFilesResponseBodyProvider] - load matching file from resource folder and use other provider to deserialize it.
 * Platform dependent realizations.
 * Other implementations might for example, read response files from other folders or external storage or deserialize other formats.
 */
interface ResponseBodyProvider {

    /**
     * This method is used to deserialize string into required object type.
     * @param type - the class to be instantiated.
     * @param genericType - genericType of the class to be instantiated.
     * @param bodyString - the content that should be deserialized into object.
     * @return deserialized object
     */
    fun <T> provideBodyObjectFromString(type: Class<T>, genericType: Type, bodyString: String): T

    /**
     * This method is used to load the content to be later deserialized.
     * It can load it from file or anywhere else. Sometimes provided [bodyInformation] might be already a required content.
     * In this case it is expected to simply return it.
     * @param bodyInformation - required information to load the content. Like path to the file or content itself.
     * @return content to be later deserialized.
     */
    fun provideBodyString(bodyInformation: String): String

    /**
     * This method is used to serialize object.
     * It is not used anywhere in the framework.
     * But it is available in [SerialisationUtils] if you would need such functionality.
     * @param value - object to be serialized.
     * @param type - type of the object.
     * @param genericType - generic type of the object.
     * @return serialized object as string.
     */
    fun <T> objectToString(value: T, type:Class<T>, genericType: Type): String
}