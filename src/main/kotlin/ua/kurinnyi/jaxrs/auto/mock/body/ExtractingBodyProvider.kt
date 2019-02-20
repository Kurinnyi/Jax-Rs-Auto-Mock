package ua.kurinnyi.jaxrs.auto.mock.body

interface ExtractingBodyProvider : BodyProvider {
    fun canExtract(jsonInfo:String): Boolean
}