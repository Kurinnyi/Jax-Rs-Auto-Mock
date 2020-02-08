package ua.kurinnyi.jaxrs.auto.mock.mocks

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.DependenciesRegistry
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.MethodStub
import ua.kurinnyi.jaxrs.auto.mock.recorder.Recorder
import java.lang.reflect.Method
import javax.servlet.http.HttpServletResponse

class ApiAdapterForResponseGeneration(
        val method: Method,
        private val response: HttpServletResponse,
        private val methodInvocationValues: Array<Any?>,
        private val dependenciesRegistry: DependenciesRegistry) {

    var shouldFlush = false

    fun <T> getObjectFromJson(bodyProvider:BodyProvider, jsonInfo:String, templateArgs: Map<String, Any>): T {
        return dependenciesRegistry.jsonUtils()
                .getObjectFromJson(bodyProvider, jsonInfo, templateArgs, method.returnType, method.genericReturnType) as T
    }

    fun <T> getObjectFromJson(jsonInfo:String, templateArgs: Map<String, Any>): T {
        return dependenciesRegistry.jsonUtils()
                .getObjectFromJson(jsonInfo, templateArgs, method.returnType, method.genericReturnType) as T
    }

    fun setResponseCode(code: Int) {
        response.status = code
        shouldFlush = true
    }

    fun writeBodyRaw(body: String) {
        IOUtils.write(body, response.writer)
        shouldFlush = true
    }

    fun proxyTo(path: String) {
        dependenciesRegistry.requestProxy().forwardRequest(path)
    }

    fun recordResponse(argumentMatchers: List<MethodStub.ArgumentMatcher>) {
        dependenciesRegistry.recorder().write(method, getParamsConfigForRecorder(argumentMatchers))
    }

    fun header(headerName: String, headerValue: String) {
        response.addHeader(headerName, headerValue)
    }

    private fun getParamsConfigForRecorder(argumentMatchers: List<MethodStub.ArgumentMatcher>):List<Recorder.MethodParam>{
        return methodInvocationValues.zip(argumentMatchers).mapIndexed { i, (argValue, argMatcher) ->
            when {
                argMatcher.matchType == MethodStub.MatchType.IGNORE_IN_RECORD ->
                    Recorder.MethodParam(Recorder.ParamProcessingWay.IGNORE, null)
                dependenciesRegistry.platformUtils().isHttpBody(method.parameters[i]) ->
                    Recorder.MethodParam(Recorder.ParamProcessingWay.BODY, null)
                else ->
                    Recorder.MethodParam(Recorder.ParamProcessingWay.PARAM, argValue.toString())
            }

        }
    }
}