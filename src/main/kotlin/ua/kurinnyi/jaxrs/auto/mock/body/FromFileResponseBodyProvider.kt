package ua.kurinnyi.jaxrs.auto.mock.body

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.MockNotFoundException
import java.lang.reflect.Type

class FromFileResponseBodyProvider(private val responseBodyProvider: ResponseBodyProvider): ResponseBodyProvider {
    override fun <T> provideBodyObjectFromString(type: Class<T>, genericType: Type, bodyString: String):T =
        responseBodyProvider.provideBodyObjectFromString(type, genericType, bodyString)

    override fun provideBodyString(bodyInformation: String): String =
        if (bodyInformation.startsWith("/")) {
            readFile(bodyInformation)
        } else {
            bodyInformation
        }

    private fun readFile(fileName: String): String {
        val fileAsStream = this.javaClass.getResourceAsStream(fileName)
                ?: throw MockNotFoundException("File $fileName not found")
        return fileAsStream.use { IOUtils.toString(it) }
    }

    override fun <T> objectToString(value: T, type: Class<T>, genericType: Type): String {
        return responseBodyProvider.objectToString(value, type, genericType)
    }
}