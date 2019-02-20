package ua.kurinnyi.jaxrs.auto.mock.body

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.StubNotFoundException
import java.lang.reflect.Type

class FileBodyProvider(private val bodyProvider: BodyProvider) : ExtractingBodyProvider {
    override fun canExtract(jsonInfo: String): Boolean = jsonInfo.startsWith("/")

    override fun <T> provideBodyObjectFromJson(type: Class<T>, genericType: Type, bodyJson: String) =
            bodyProvider.provideBodyObject(type, genericType, bodyJson)

    override fun provideBodyJson(body: String): String  = readFile(body)

    override fun <T> provideBodyObject(type: Class<T>, genericType: Type, body: String): T {
        return bodyProvider.provideBodyObject(type, genericType, readFile(body))
    }

    private fun readFile(fileName: String): String {
        val jsonAsStream = this.javaClass.getResourceAsStream(fileName)
                ?: throw StubNotFoundException("Json file $fileName not found")
        return jsonAsStream.use { IOUtils.toString(it) }
    }
}