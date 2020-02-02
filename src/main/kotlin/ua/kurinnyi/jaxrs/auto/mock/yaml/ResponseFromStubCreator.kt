package ua.kurinnyi.jaxrs.auto.mock.yaml

import ua.kurinnyi.jaxrs.auto.mock.body.JacksonBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.JerseyInternalBodyProvider
import java.lang.reflect.Method
import javax.servlet.http.HttpServletResponse
import ua.kurinnyi.kotlin.patternmatching.PatternMatching.match

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
                method.returnType.match {
                    case(String::class.java).then { body }
                    case(Int::class.java).then { body.toInt() }
                    case(Double::class.java).then { body.toDouble() }
                    case(ByteArray::class.java).then { body.toByteArray() }
                    case<Class<*>>().and { name == "void" }.then { "" }
                    case<Class<*>>().and { useJerseyDeserialization }.then {
                        JerseyInternalBodyProvider.provideBodyObject(method.returnType, method.genericReturnType, body)
                    }
                    otherwise {
                        JacksonBodyProvider.provideBodyObject(method.returnType, method.genericReturnType, body)
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Can't map stub into response object", e)
            }

    private fun setHeaders(servletResponse: HttpServletResponse, headers: List<YamlMethodStub.Header>) {
        headers.forEach { (name, value) -> servletResponse.addHeader(name, value) }
    }
}
