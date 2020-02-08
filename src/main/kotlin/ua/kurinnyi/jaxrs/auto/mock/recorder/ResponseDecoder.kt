package ua.kurinnyi.jaxrs.auto.mock.recorder

interface ResponseDecoder {

    fun decodeToString(response: ByteArray):String
    fun encodings():List<String>


    object NoEncodingDecoder : ResponseDecoder {
        override fun encodings(): List<String> = emptyList()

        override fun decodeToString(response: ByteArray) = String(response)
    }
}