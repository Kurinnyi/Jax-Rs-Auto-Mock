package ua.kurinnyi.jaxrs.auto.mock.yaml

import ua.kurinnyi.jaxrs.auto.mock.body.JacksonBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.JerseyInternalBodyProvider
import java.lang.reflect.Method
import javax.servlet.http.HttpServletResponse

object ResponseFromStubCreator {
    var useJerseyDeserialization: Boolean = false

    fun getResponseObject(method: Method, response: YamlMethodStub.Response, httpResponse: HttpServletResponse): Any? {
        response.headers?.let { setHeaders(httpResponse, it) }
        response.code?.let { setStatus(httpResponse, it) }
        return response.body?.let { parseBody(method, it) }
    }

    private fun setStatus(servletResponse: HttpServletResponse, statusCode: Int) {
        servletResponse.status = statusCode
        servletResponse.flushBuffer()
    }

    private fun parseBody(method: Method, body: String): Any =
            try {
                when (method.returnType) {
                    String::class.java -> body
                    Int::class.java -> body.toInt()
                    Double::class.java -> body.toDouble()
                    ByteArray::class.java -> body.toByteArray()
                    else -> {
                        val bodyProvider = if (useJerseyDeserialization) JerseyInternalBodyProvider else JacksonBodyProvider
                        bodyProvider.provideBodyObject(method.returnType, method.genericReturnType, body)
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Can't map stub into response object", e)
            }

    private fun setHeaders(servletResponse: HttpServletResponse, headers: List<YamlMethodStub.Header>) {
        headers.forEach { (name, value) -> servletResponse.addHeader(name, value) }
    }
}
