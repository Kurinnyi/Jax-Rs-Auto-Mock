package ua.kurinnyi.jaxrs.auto.mock.body

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.StubNotFoundException
import java.lang.reflect.Type

class FileBodyProvider(private val bodyProvider: BodyProvider): BodyProvider {
    override fun <T> provideBodyObjectFromString(type: Class<T>, genericType: Type, bodyString: String):T =
        bodyProvider.provideBodyObjectFromString(type, genericType, bodyString)

    override fun provideBodyString(bodyInformation: String): String =
        if (bodyInformation.startsWith("/")) {
            readFile(bodyInformation)
        } else {
            bodyInformation
        }

    private fun readFile(fileName: String): String {
        val fileAsStream = this.javaClass.getResourceAsStream(fileName)
                ?: throw StubNotFoundException("File $fileName not found")
        return fileAsStream.use { IOUtils.toString(it) }
    }

    override fun <T> objectToString(value: T, type: Class<T>, genericType: Type): String {
        return bodyProvider.objectToString(value, type, genericType)
    }
}