package ua.kurinnyi.jaxrs.auto.mock.recorder

interface ResponseDecoder {

    fun decodeToString(response: ByteArray):String


    object NoEncodingDecoder : ResponseDecoder {
        override fun decodeToString(response: ByteArray) = String(response)
    }
}