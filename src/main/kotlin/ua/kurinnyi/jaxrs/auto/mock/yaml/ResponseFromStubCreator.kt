package ua.kurinnyi.jaxrs.auto.mock.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import ua.kurinnyi.jaxrs.auto.mock.JerseyInternalsFilter
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
                        if (useJerseyDeserialization)
                            JerseyInternalsFilter.prepareResponse(method, body)
                        else {
                            ObjectMapper().readValue(body, method.returnType)
                        }
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Can't map stub into response object", e)
            }

    private fun setHeaders(servletResponse: HttpServletResponse, headers: List<YamlMethodStub.Header>) {
        headers.forEach { (name, value) -> servletResponse.addHeader(name, value) }
    }
}
