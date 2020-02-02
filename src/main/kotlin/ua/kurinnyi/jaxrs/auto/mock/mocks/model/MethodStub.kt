package ua.kurinnyi.jaxrs.auto.mock.mocks.model

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.mocks.ApiAdapter
import java.lang.reflect.Method
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

open class MethodStub(
        private val method: Method,
        private val arguments: List<ArgumentMatcher>,
        private val requestHeaders: List<HeaderParameter>,
        private val responseSection:  (ApiAdapter, Array<Any?>) -> Any?,
        private var isActivated: Boolean = true
) : ResourceMethodStub {

    class ArgumentMatcher(internal val matchType: MatchType, internal val matcher: (Any?) -> Boolean)

    enum class MatchType {
        MATCH, BODY_MATCH, IGNORE_IN_RECORD
    }

    data class HeaderParameter(val headerName: String, val headerValue: ArgumentMatcher)

    fun activate() {
        isActivated = true
    }

    fun deactivate() {
        isActivated = false
    }

    override fun getStubbedClassName(): String = method.declaringClass.name

    override fun isMatchingMethod(method: Method, args: Array<Any?>?, request: HttpServletRequest): Boolean =
        method == this.method && paramMatch(args, request) && headersMatch(request) && isActivated

    override fun produceResponse(method: Method, args: Array<Any?>?, response: HttpServletResponse): Any? {
        val apiAdapter = ApiAdapter(method, response, args ?: emptyArray(), arguments)
        val responseObject =  responseSection(apiAdapter, args?: emptyArray())
        if (apiAdapter.shouldFlush) {
            response.flushBuffer()
        }
        return responseObject
    }

    private fun headersMatch(request: HttpServletRequest): Boolean {
        return requestHeaders.all { (headerName, headerValue) ->
            headerValue.matcher(request.getHeader(headerName))
        }
    }

    private fun paramMatch(args: Array<Any?>?, request: HttpServletRequest): Boolean {
        return if (args == null) {
            arguments.isEmpty()
        } else {
            arguments.zip(args).all { (matcher, arg) ->
                when (matcher.matchType) {
                    MatchType.MATCH, MatchType.IGNORE_IN_RECORD -> matcher.matcher(arg)
                    MatchType.BODY_MATCH -> matcher.matcher(Utils.bodyAsString(request))
                }
            }
        }
    }
}