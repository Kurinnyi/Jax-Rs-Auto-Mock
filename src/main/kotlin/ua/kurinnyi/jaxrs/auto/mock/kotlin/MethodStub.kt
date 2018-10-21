package ua.kurinnyi.jaxrs.auto.mock.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import ua.kurinnyi.jaxrs.auto.mock.JerseyInternalsFilter
import ua.kurinnyi.jaxrs.auto.mock.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.Utils
import java.lang.reflect.Method
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MethodStub(private val clazz: Class<*>, private val method: Method, val arguments: List<ArgumentMatcher>) : ResourceMethodStub {
    override fun getStubbedClassName(): String = clazz.name

    override fun isMatchingMethod(method: Method, args: Array<Any>?, request: HttpServletRequest): Boolean {
        return method == this.method && paramMatch(args, request) && headersMatch(request)
    }

    override fun produceResponse(method: Method, response: HttpServletResponse): Any? {
        responseHeaders.forEach { (name, value) -> response.addHeader(name, value) }
        code?.let {
            response.status = it
            response.flushBuffer()
        }
        return when {
            body != null -> body
            bodyJson != null -> ObjectMapper().readValue(bodyJson, method.returnType)
            bodyJsonJersey != null -> JerseyInternalsFilter.prepareResponse(method, bodyJsonJersey!!)
            bodyProvider != null -> bodyProvider!!()
            else -> null
        }
    }

    internal val requestHeaders: MutableList<HeaderParameter> = mutableListOf()
    internal var code: Int? = null
    internal var body: Any? = null
    internal var bodyJson: String? = null
    internal var bodyJsonJersey: String? = null
    internal var bodyProvider: (() -> Any?)? = null
    internal val responseHeaders: MutableMap<String, String> = mutableMapOf()

    class ArgumentMatcher(internal val matchType: MatchType, internal val matcher: (Any?) -> Boolean)

    enum class MatchType {
        MATCH, BODY_MATCH
    }

    data class HeaderParameter(val headerName: String, val headerValue: ArgumentMatcher)

    private fun headersMatch(request: HttpServletRequest): Boolean {
        return requestHeaders.all { (headerName, headerValue) ->
            headerValue.matcher(request.getHeader(headerName))
        }
    }

    private fun paramMatch(args: Array<Any>?, request: HttpServletRequest): Boolean {
        return if (args == null) {
            arguments.isEmpty()
        } else {
            arguments.zip(args).all { (matcher, arg) ->
                when (matcher.matchType) {
                    MatchType.MATCH -> matcher.matcher(arg)
                    MatchType.BODY_MATCH -> matcher.matcher(Utils.bodyAsString(request))
                }
            }
        }
    }
}