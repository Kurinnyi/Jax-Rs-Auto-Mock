package ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl

import ua.kurinnyi.jaxrs.auto.mock.DependenciesRegistry
import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.MethodMock
import java.lang.reflect.Method
import javax.servlet.http.HttpServletRequest

open class ExecutableMethodMock(
        private val method: Method,
        val argumentsMatchers: List<ArgumentMatcher>,
        private val requestHeadersMatchers: List<HeaderParameter>,
        private val responseSection:  (ApiAdapterForResponseGeneration, Array<Any?>, ExecutableMethodMock) -> Any?,
        private var isActivated: Boolean = true
) : MethodMock {

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

    override fun getMockedClassName(): String = method.declaringClass.name

    override fun isMatchingMethod(method: Method, receivedArguments: Array<Any?>?, dependenciesRegistry: DependenciesRegistry): Boolean {
        val request = dependenciesRegistry.httpRequestResponseHolder().request
        return method == this.method && paramMatch(receivedArguments, request) && headersMatch(request) && isActivated
    }

    override fun produceResponse(
            method: Method,
            receivedArguments: Array<Any?>?,
            dependenciesRegistry: DependenciesRegistry): Any? {
        val response = dependenciesRegistry.httpRequestResponseHolder().response
        val apiAdapter = ApiAdapterForResponseGeneration(method, response, receivedArguments
                ?: emptyArray(), dependenciesRegistry)
        val responseObject =  responseSection(apiAdapter, receivedArguments?: emptyArray(), this)
        if (apiAdapter.shouldFlush) {
            response.flushBuffer()
        }
        return responseObject
    }

    private fun headersMatch(request: HttpServletRequest): Boolean {
        return requestHeadersMatchers.all { (headerName, headerValue) ->
            headerValue.matcher(request.getHeader(headerName))
        }
    }

    private fun paramMatch(args: Array<Any?>?, request: HttpServletRequest): Boolean {
        return if (args == null) {
            argumentsMatchers.isEmpty()
        } else {
            argumentsMatchers.zip(args).all { (matcher, arg) ->
                when (matcher.matchType) {
                    MatchType.MATCH, MatchType.IGNORE_IN_RECORD -> matcher.matcher(arg)
                    MatchType.BODY_MATCH -> matcher.matcher(Utils.bodyAsString(request))
                }
            }
        }
    }
}