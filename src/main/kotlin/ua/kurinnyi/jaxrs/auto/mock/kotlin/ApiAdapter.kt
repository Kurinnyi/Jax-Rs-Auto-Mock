package ua.kurinnyi.jaxrs.auto.mock.kotlin

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.RequestProxy
import ua.kurinnyi.jaxrs.auto.mock.recorder.Recorder
import java.lang.reflect.Method
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam

class ApiAdapter(
        val method: Method,
        private val response: HttpServletResponse,
        private val methodInvocationValues: Array<Any?>,
        private val argumentMatchers: List<MethodStub.ArgumentMatcher>) {

    var shouldFlush = false

    fun <T> getObjectFromJson(bodyProvider:BodyProvider, jsonInfo:String, templateArgs: Map<String, Any>): T {
        return JsonUtils.getObjectFromJson(bodyProvider, jsonInfo, templateArgs, method.returnType, method.genericReturnType) as T
    }

    fun <T> getObjectFromJson(jsonInfo:String, templateArgs: Map<String, Any>): T {
        return JsonUtils.getObjectFromJson(jsonInfo, templateArgs, method.returnType, method.genericReturnType) as T
    }

    fun code(code: Int) {
        response.status = code
        shouldFlush = true
    }

    fun bodyRaw(body: String) {
        IOUtils.write(body, response.writer)
        shouldFlush = true
    }

    fun proxyTo(path: String) {
        RequestProxy.forwardRequest(path)
    }

    fun header(headerName: String, headerValue: String) {
        response.addHeader(headerName, headerValue)
    }

    fun getParamsConfigForRecorder():List<Recorder.MethodParam>{
        return methodInvocationValues.zip(argumentMatchers).mapIndexed { i, (argValue, argMatcher) ->
            when {
                argMatcher.matchType == MethodStub.MatchType.IGNORE_IN_RECORD ->
                    Recorder.MethodParam(Recorder.ParamProcessingWay.IGNORE, null)
                notBody(i) ->
                    Recorder.MethodParam(Recorder.ParamProcessingWay.PARAM, argValue.toString())
                else ->
                    Recorder.MethodParam(Recorder.ParamProcessingWay.BODY, null)
            }

        }

    }

    private fun notBody(i: Int) =
            method.parameters[i].annotations.any { it is QueryParam || it is PathParam }
}