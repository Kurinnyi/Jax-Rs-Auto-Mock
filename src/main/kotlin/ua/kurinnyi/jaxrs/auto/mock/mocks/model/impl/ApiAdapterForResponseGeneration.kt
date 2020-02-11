package ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.DependenciesRegistry
import ua.kurinnyi.jaxrs.auto.mock.extensions.ResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.response.Recorder
import java.lang.reflect.Method
import javax.servlet.http.HttpServletResponse

class ApiAdapterForResponseGeneration(
        val method: Method,
        private val response: HttpServletResponse,
        private val methodInvocationParameters: Array<Any?>,
        private val dependenciesRegistry: DependenciesRegistry) {

    var shouldFlush = false

    fun <T> getObjectFromString(responseBodyProvider: ResponseBodyProvider, stringInfo:String, templateArgs: Map<String, Any>): T {
        return dependenciesRegistry.serialisationUtils()
                .getObjectFromString(responseBodyProvider, stringInfo, templateArgs, method.returnType, method.genericReturnType) as T
    }

    fun <T> getObjectFromString(objectInfo:String, templateArgs: Map<String, Any>): T {
        return dependenciesRegistry.serialisationUtils()
                .getObjectFromString(objectInfo, templateArgs, method.returnType, method.genericReturnType) as T
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

    fun recordResponse(argumentMatchers: List<ExecutableMethodMock.ArgumentMatcher>) {
        dependenciesRegistry.recorder().write(method, getParamsConfigForRecorder(argumentMatchers))
    }

    fun setResponseHeader(headerName: String, headerValue: String) {
        response.addHeader(headerName, headerValue)
    }

    private fun getParamsConfigForRecorder(argumentMatchers: List<ExecutableMethodMock.ArgumentMatcher>):List<Recorder.MethodParam>{
        return methodInvocationParameters.zip(argumentMatchers).mapIndexed { i, (argValue, argMatcher) ->
            when {
                argMatcher.matchType == ExecutableMethodMock.MatchType.IGNORE_IN_RECORD ->
                    Recorder.MethodParam(Recorder.ParamProcessingWay.IGNORE, null)
                dependenciesRegistry.platformUtils().isHttpBody(method.parameters[i]) ->
                    Recorder.MethodParam(Recorder.ParamProcessingWay.BODY, null)
                else ->
                    Recorder.MethodParam(Recorder.ParamProcessingWay.PARAM, argValue.toString())
            }

        }
    }
}