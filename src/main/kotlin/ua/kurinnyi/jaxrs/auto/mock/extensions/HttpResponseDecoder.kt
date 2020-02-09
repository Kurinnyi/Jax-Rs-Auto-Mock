package ua.kurinnyi.jaxrs.auto.mock.extensions

interface HttpResponseDecoder {

    fun decodeToString(response: ByteArray):String
    fun encodings():List<String>

    object NoEncodingDecoder : HttpResponseDecoder {
        override fun encodings(): List<String> = emptyList()

        override fun decodeToString(response: ByteArray) = String(response)
    }
}