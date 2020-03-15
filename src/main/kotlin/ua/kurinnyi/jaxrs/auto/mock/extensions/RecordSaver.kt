package ua.kurinnyi.jaxrs.auto.mock.extensions

import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMocksHolder
import ua.kurinnyi.jaxrs.auto.mock.extensions.defaul.ConsoleRecordSaver

/**
 * This interface specifies the way to store recorded responses/requests.
 * Default implementation is [ConsoleRecordSaver] which simply writes the records into the console.
 * You might want to implement this interface to store records in some file or even some external system.
 */
interface RecordSaver {

    /**
     * Save the record somewhere in this method.
     * It is invoked with all the records for a method every time any of the records for the method is changed or added.
     * So it is expected to completely rewrite all the records for the method on every invocation.
     * @param mocks - all collected records for some method
     * @param objectMapper - the object that can be used to serialize mocks. It is same as in [SerialisedMocksLoader] to have same format for
     * read and write of the mocks.
     */
    fun saveRecords(mocks: SerializableMocksHolder, objectMapper: SerializableObjectMapper)
}