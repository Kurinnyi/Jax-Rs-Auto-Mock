package ua.kurinnyi.jaxrs.auto.mock.extensions

/**
 * This interface is HTTP content decoder for recorder.
 * When proxying request to some external system, response might be encoded in some way.
 * To be able to record it as a text, it should be decoded.
 * Implementations of this interface should do this decoding.
 */
interface HttpResponseDecoder {

    /**
     * Do the decoding in this method.
     * @param response - http response as array of bytes to be decoded.
     * @return decoded http response as string.
     */
    fun decodeToString(response: ByteArray):String

    /**
     * @return List of encoding that are supported by this decoder.
     */
    fun encodings():List<String>

    object NoEncodingDecoder : HttpResponseDecoder {
        override fun encodings(): List<String> = emptyList()

        override fun decodeToString(response: ByteArray) = String(response)
    }
}