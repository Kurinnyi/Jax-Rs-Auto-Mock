package ua.kurinnyi.jaxrs.auto.mock.serializable

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ApiAdapterForResponseGeneration
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ExecutableMethodMock
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMethodMock.RequestParameter.MatchType.*
import ua.kurinnyi.kotlin.patternmatching.PatternMatching.match
import java.lang.reflect.Method

class SerializableMockToExecutableMethodMockConverter {
    fun toExecutableMock(serializableMethodMock:SerializableMethodMock): List<ExecutableMethodMock> {
        return serializableMethodMock.cases.map { case ->
            ExecutableMethodMock(
                    getMethod(serializableMethodMock.className, case, serializableMethodMock.methodName),
                    getParamsMatchers(case.request.methodParameters),
                    getHeaderMatchers(case.request.headerParameters),
                    { apiAdapterForResponseGeneration, _, _ -> getResponse(case.response, apiAdapterForResponseGeneration) })
        }
    }

    private fun getResponse(response: SerializableMethodMock.Response, apiAdapterForResponseGeneration: ApiAdapterForResponseGeneration): Any? {
        response.code?.let { apiAdapterForResponseGeneration.setResponseCode(it) }
        response.headers?.forEach { apiAdapterForResponseGeneration.setResponseHeader(it.name, it.value) }
        return response.body?.let { apiAdapterForResponseGeneration.getObjectFromString(it, emptyMap()) }
    }

    private fun getHeaderMatchers(headerParameters: List<SerializableMethodMock.HeaderParameter>?): List<ExecutableMethodMock.HeaderParameter> {
        return (headerParameters ?: emptyList()).map {
            ExecutableMethodMock.HeaderParameter(it.headerName, ExecutableMethodMock.ArgumentMatcher(ExecutableMethodMock.MatchType.MATCH) { param ->
                isParameterMatches(it.headerValue, param)
            })
        }
    }

    private fun getParamsMatchers(methodParameters: List<SerializableMethodMock.RequestParameter>?): List<ExecutableMethodMock.ArgumentMatcher> {
        return (methodParameters ?: emptyList()).map {
            val matchType = when (it.matchType) {
                BODY_TEMPLATE, BODY -> ExecutableMethodMock.MatchType.BODY_MATCH
                else -> ExecutableMethodMock.MatchType.MATCH
            }
            ExecutableMethodMock.ArgumentMatcher(matchType) { param -> isParameterMatches(it, param) }
        }
    }

    private fun getMethod(className: String, case: SerializableMethodMock.Case, methodName: String): Method {
        val clazz = this.javaClass.classLoader.loadClass(className)
        val methodParameters = case.request.methodParameters ?: emptyList()
        return (clazz.methods.first { it.name == methodName && methodParameters.size == it.parameters.size }
                ?: throw IllegalArgumentException("Can't find class method $methodName with ${methodParameters.size} parameters"))
    }

    private fun isParameterMatches(requestParameter: SerializableMethodMock.RequestParameter, methodReceivedParameter: Any?): Boolean {
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