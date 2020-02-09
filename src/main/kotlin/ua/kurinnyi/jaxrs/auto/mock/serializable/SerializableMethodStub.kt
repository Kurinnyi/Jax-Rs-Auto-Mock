package ua.kurinnyi.jaxrs.auto.mock.serializable

data class MethodStubsHolder(val stubs: List<SerializableMethodStub>)

data class SerializableMethodStub(val className: String, val methodName: String, val cases: List<Case>) {
    data class RequestParameter(val matchType: MatchType, val value: String?) {
        enum class MatchType {
            EXACT, TEMPLATE, BODY_TEMPLATE, BODY, IS_NULL, NOT_NULL, ANY;
        }
    }

    data class HeaderParameter(val headerName: String, val headerValue: RequestParameter)

    data class Response(val code: Int?, val headers: List<Header>?, var body: String?)

    data class Header(val name: String, val value: String)

    data class Request(val methodParameters: List<RequestParameter>?, val headerParameters: List<HeaderParameter>?)

    data class Case(val request: Request, val response: Response)
}

