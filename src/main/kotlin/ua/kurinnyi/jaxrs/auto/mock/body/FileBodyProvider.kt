package ua.kurinnyi.jaxrs.auto.mock.body

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.StubNotFoundException
import java.lang.reflect.Type

class FileBodyProvider(private val bodyProvider: BodyProvider): BodyProvider {
    override fun <T> provideBodyObjectFromJson(type: Class<T>, genericType: Type, bodyJson: String):T =
        bodyProvider.provideBodyObjectFromJson(type, genericType, bodyJson)

    override fun provideBodyJson(body: String): String =
        if (body.startsWith("/")) {
            readFile(body)
        } else {
            body
        }

    private fun readFile(fileName: String): String {
        val jsonAsStream = this.javaClass.getResourceAsStream(fileName)
                ?: throw StubNotFoundException("Json file $fileName not found")
        return jsonAsStream.use { IOUtils.toString(it) }
    }

    override fun <T> objectToJson(value: T, type: Class<T>, genericType: Type): String {
        return bodyProvider.objectToJson(value, type, genericType)
    }
}