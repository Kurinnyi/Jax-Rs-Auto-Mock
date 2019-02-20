package ua.kurinnyi.jaxrs.auto.mock.kotlin

import ua.kurinnyi.jaxrs.auto.mock.model.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.Utils
import java.lang.IllegalStateException
import java.lang.reflect.Method
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MethodStub(private val clazz: Class<*>, private val method: Method, val arguments: List<ArgumentMatcher>) : ResourceMethodStub {

    internal val requestHeaders: MutableList<HeaderParameter> = mutableListOf()
    internal var responseSection:  (MethodStubDefinitionResponseContext<*>.(Array<Any?>) -> Any?)? = null
    internal var responseSection1: (MethodStubDefinitionResponseContext<*>.(Any?) -> Any?)? = null
    internal var responseSection2: (MethodStubDefinitionResponseContext<*>.(Any?,Any?) -> Any?)? = null
    internal var responseSection3: (MethodStubDefinitionResponseContext<*>.(Any?,Any?,Any?) -> Any?)? = null
    internal var responseSection4: (MethodStubDefinitionResponseContext<*>.(Any?,Any?,Any?,Any?) -> Any?)? = null
    internal var responseSection5: (MethodStubDefinitionResponseContext<*>.(Any?,Any?,Any?,Any?,Any?) -> Any?)? = null
    internal var isActivatedByGroups: Boolean = true


    override fun getStubbedClassName(): String = clazz.name

    override fun isMatchingMethod(method: Method, args: Array<Any?>?, request: HttpServletRequest): Boolean {
        return method == this.method && paramMatch(args, request) && headersMatch(request) && isActivatedByGroups
    }

    override fun produceResponse(method: Method, args: Array<Any?>?, response: HttpServletResponse): Any? {
        val apiAdapter = ApiAdapterFactory.getApiAdapter(method, response)
        val responseContext = MethodStubDefinitionResponseContext<Any?>(apiAdapter)
        val a = args?: emptyArray()
        val responseObject =  when {
            responseSection != null -> responseSection!!(responseContext, a)
            responseSection1 != null -> responseSection1!!(responseContext, a[0])
            responseSection1 != null -> responseSection2!!(responseContext, a[0], a[1])
            responseSection2 != null -> responseSection3!!(responseContext, a[0], a[1], a[2])
            responseSection3 != null -> responseSection4!!(responseContext, a[0], a[1], a[2], a[3])
            responseSection4 != null -> responseSection5!!(responseContext, a[0], a[1], a[2], a[3], a[4])
            else -> throw IllegalStateException("Looks imposible")
        }
        if (apiAdapter.shouldFlush) {
            response.flushBuffer()
        }
        return responseObject
    }

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

    private fun paramMatch(args: Array<Any?>?, request: HttpServletRequest): Boolean {
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