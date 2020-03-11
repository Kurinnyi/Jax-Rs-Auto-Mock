package ua.kurinnyi.jaxrs.auto.mock.extensions

import ua.kurinnyi.jaxrs.auto.mock.extensions.defaul.ResourceFolderSerialisedMocksLoader
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMethodMock

/**
 * This interface is used to load manually writen or recorded serializable mocks.
 * Default implementation is [ResourceFolderSerialisedMocksLoader] configured to load yaml formatted mocks from resource/mocks folder.
 * Your implementation might read mocks from different folders or even external system.
 */
interface SerialisedMocksLoader {

    /**
     * Method to load serialized mocks.
     * It is invoked repeatable to enable hot reload of this kind of mocks.
     * @param serializableObjectMapper - the object that can be used to deserialize mocks. It is same as in [serializableObjectMapper] to have
     * same format for read and write of the mocks.
     * @return loaded list of mocks.
     */
    fun reloadMocks(serializableObjectMapper: SerializableObjectMapper): List<SerializableMethodMock>
}