package ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl

import ua.kurinnyi.jaxrs.auto.mock.DependenciesRegistry
import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.mocks.ApiAdapterForResponseGeneration
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.ResourceMethodStub
import java.lang.reflect.Method
import javax.servlet.http.HttpServletRequest

open class MethodStub(
        private val method: Method,
        val arguments: List<ArgumentMatcher>,
        private val requestHeaders: List<HeaderParameter>,
        private val responseSection:  (ApiAdapterForResponseGeneration, Array<Any?>, MethodStub) -> Any?,
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

    override fun isMatchingMethod(method: Method, args: Array<Any?>?, dependenciesRegistry: DependenciesRegistry): Boolean {
        val request = dependenciesRegistry.contextSaveFilter().request
        return method == this.method && paramMatch(args, request) && headersMatch(request) && isActivated
    }

    override fun produceResponse(
            method: Method,
            args: Array<Any?>?,
            dependenciesRegistry: DependenciesRegistry): Any? {
        val response = dependenciesRegistry.contextSaveFilter().response
        val apiAdapter = ApiAdapterForResponseGeneration(method, response, args ?: emptyArray(), dependenciesRegistry)
        val responseObject =  responseSection(apiAdapter, args?: emptyArray(), this)
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