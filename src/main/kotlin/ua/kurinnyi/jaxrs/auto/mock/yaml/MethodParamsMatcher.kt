package ua.kurinnyi.jaxrs.auto.mock.yaml

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.Utils.trimToSingleSpaces
import ua.kurinnyi.jaxrs.auto.mock.yaml.YamlMethodStub.RequestParameter.MatchType.*
import ua.kurinnyi.kotlin.patternmatching.PatternMatching.match
import javax.servlet.http.HttpServletRequest

object MethodParamsMatcher {

    fun isParamsMatches(methodReceivedParameters: Array<Any?>?, request: YamlMethodStub.Request?, httpRequest: HttpServletRequest) =
            if (request == null) {
                true
            } else {
                isHeaderParamsMatches(request.headerParameters, httpRequest) &&
                        if (methodReceivedParameters == null) {
                            request.methodParameters == null || request.methodParameters.isEmpty()
                        } else {
                            isMethodParamsMatches(methodReceivedParameters, request.methodParameters, httpRequest)
                        }
            }

    private fun isHeaderParamsMatches(headerParameters: List<YamlMethodStub.HeaderParameter>?, httpRequest: HttpServletRequest): Boolean {
        return headerParameters == null || headerParameters.all { (headerName, headerValue) ->
            isParameterMatches(headerValue, httpRequest.getHeader(headerName), httpRequest)
        }
    }

    private fun isMethodParamsMatches(methodReceivedParams: Array<Any?>,
                                      methodExpectedParams: List<YamlMethodStub.RequestParameter>?,
                                      httpRequest: HttpServletRequest): Boolean {
        return methodExpectedParams == null ||
                methodReceivedParams.size == methodExpectedParams.size
                && methodExpectedParams.zip(methodReceivedParams).all { (expected, received) ->
                    isParameterMatches(expected, received, httpRequest)
                }
    }

    private fun isParameterMatches(requestParameter: YamlMethodStub.RequestParameter, methodReceivedParameter: Any?, httpRequest: HttpServletRequest): Boolean {
        val matchType = requestParameter.matchType
        val expectedValue = requestParameter.value
        return matchType.match {
            case(ANY).then { true }
            case(IS_NULL).then { methodReceivedParameter == null }
            case<Any>().and { methodReceivedParameter == null }.then { false }
            case(NOT_NULL).then { true }
            case(EXACT).then { expectedValue == methodReceivedParameter.toString() }
            case(TEMPLATE).then { methodReceivedParameter.toString().matches(expectedValue!!.toRegex()) }
            case(BODY).then { trimToSingleSpaces(expectedValue!!) == trimToSingleSpaces(Utils.bodyAsString(httpRequest)) }
            case(BODY_TEMPLATE).then { Utils.bodyAsString(httpRequest).matches(expectedValue!!.toRegex()) }
            otherwise {
                throw RuntimeException("""Currently supported match types are only:ANY,
                |IS_NULL, NOT_NULL, EXACT, TEMPLATE, BODY, BODY_TEMPLATE""".trimMargin())
            }
        }

    }
}
