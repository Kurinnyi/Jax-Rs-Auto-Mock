package ua.kurinnyi.jaxrs.auto.mock.serializable

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ApiAdapterForResponseGeneration
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.MethodStub
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMethodStub.RequestParameter.MatchType.*
import ua.kurinnyi.kotlin.patternmatching.PatternMatching.match
import java.lang.reflect.Method

class SerializableToMethodStubConverter {
    fun toMethodStubs(serializableMethodStub:SerializableMethodStub): List<MethodStub> {
        return serializableMethodStub.cases.map { case ->
            MethodStub(
                    getMethod(serializableMethodStub.className, case, serializableMethodStub.methodName),
                    getParamsMatchers(case.request.methodParameters),
                    getHeaderMatchers(case.request.headerParameters),
                    { apiAdapterForResponseGeneration, _, _ -> getResponse(case.response, apiAdapterForResponseGeneration) })
        }
    }

    private fun getResponse(response: SerializableMethodStub.Response, apiAdapterForResponseGeneration: ApiAdapterForResponseGeneration): Any? {
        response.code?.let { apiAdapterForResponseGeneration.setResponseCode(it) }
        response.headers?.forEach { apiAdapterForResponseGeneration.header(it.name, it.value) }
        return response.body?.let { apiAdapterForResponseGeneration.getObjectFromString(it, emptyMap()) }
    }

    private fun getHeaderMatchers(headerParameters: List<SerializableMethodStub.HeaderParameter>?): List<MethodStub.HeaderParameter> {
        return (headerParameters ?: emptyList()).map {
            MethodStub.HeaderParameter(it.headerName, MethodStub.ArgumentMatcher(MethodStub.MatchType.MATCH) { param ->
                isParameterMatches(it.headerValue, param)
            })
        }
    }

    private fun getParamsMatchers(methodParameters: List<SerializableMethodStub.RequestParameter>?): List<MethodStub.ArgumentMatcher> {
        return (methodParameters ?: emptyList()).map {
            val matchType = when (it.matchType) {
                BODY_TEMPLATE, BODY -> MethodStub.MatchType.BODY_MATCH
                else -> MethodStub.MatchType.MATCH
            }
            MethodStub.ArgumentMatcher(matchType) { param -> isParameterMatches(it, param) }
        }
    }

    private fun getMethod(className: String, case: SerializableMethodStub.Case, methodName: String): Method {
        val clazz = this.javaClass.classLoader.loadClass(className)
        val methodParameters = case.request.methodParameters ?: emptyList()
        return (clazz.methods.first { it.name == methodName && methodParameters.size == it.parameters.size }
                ?: throw IllegalArgumentException("Can't find class method $methodName with ${methodParameters.size} parameters"))
    }

    private fun isParameterMatches(requestParameter: SerializableMethodStub.RequestParameter, methodReceivedParameter: Any?): Boolean {
        val expectedValue = requestParameter.value
        return requestParameter.matchType.match {
            case(ANY).then { true }
            case(IS_NULL).then { methodReceivedParameter == null }
            case<Any>().and { methodReceivedParameter == null }.then { false }
            case(NOT_NULL).then { true }
            case(EXACT).then { expectedValue == methodReceivedParameter.toString() }
            case(TEMPLATE, BODY_TEMPLATE).then { methodReceivedParameter.toString().matches(expectedValue!!.toRegex()) }
            case(BODY).then { Utils.trimToSingleSpaces(expectedValue!!) == Utils.trimToSingleSpaces(methodReceivedParameter.toString()) }
            otherwise {
                throw RuntimeException("""Currently supported match types are only:ANY,
                |IS_NULL, NOT_NULL, EXACT, TEMPLATE, BODY, BODY_TEMPLATE""".trimMargin())
            }
        }

    }
}