package ua.kurinnyi.jaxrs.auto.mock.yaml

import ua.kurinnyi.jaxrs.auto.mock.ResourceMethodStub
import java.lang.reflect.Method
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

data class MethodStubsHolder(val stubs: List<YamlMethodStub>)

data class FlatYamlMethodStub(val className: String, val methodName: String, val case: YamlMethodStub.Case) : ResourceMethodStub {
    override fun getStubbedClassName() = className

    override fun isMatchingMethod(method: Method, args: Array<Any?>?, request: HttpServletRequest): Boolean {
        return method.declaringClass.name == className
                && method.name == methodName
                && MethodParamsMatcher.isParamsMatches(args, case.request, request)
    }

    override fun produceResponse(method: Method, args: Array<Any?>?, response: HttpServletResponse): Any? {
        return ResponseFromStubCreator.getResponseObject(method, case.response, response)
    }


}

data class YamlMethodStub(val className: String, val methodName: String, val cases: List<Case>) {

    fun toFlatStubs():List<FlatYamlMethodStub> {
        return cases.map { FlatYamlMethodStub(className, methodName, it) }
    }

    data class RequestParameter(val matchType: MatchType, val value: String?) {
        enum class MatchType {
            EXACT, TEMPLATE, BODY_TEMPLATE, BODY, IS_NULL, NOT_NULL, ANY;
        }
    }

    data class HeaderParameter(val headerName: String, val headerValue: RequestParameter)

    data class Response(val code: Int?, val headers: List<Header>?, var body: String?)

    data class Header(val name: String, val value: String)

    data class Request(val methodParameters: List<RequestParameter>?, val headerParameters: List<HeaderParameter>?)

    data class Case(val request: Request?, val response: Response)
}

