package ua.kurinnyi.jaxrs.auto.mock.recorder

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.Utils.bodyAsString
import ua.kurinnyi.jaxrs.auto.mock.filters.BufferingFilter
import ua.kurinnyi.jaxrs.auto.mock.filters.ContextSaveFilter
import ua.kurinnyi.jaxrs.auto.mock.filters.ResponseIntersectingFilter
import ua.kurinnyi.jaxrs.auto.mock.yaml.MethodStubsHolder
import ua.kurinnyi.jaxrs.auto.mock.yaml.YamlMethodStub
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object Recorder {

    val responseDecoders = mutableMapOf<String, ResponseDecoder>()
    var recordSaver: RecordSaver = ConsoleRecordSaver

    private val recordedCalls = ConcurrentHashMap<Method, Map<YamlMethodStub.Request, YamlMethodStub.Response>>()

    fun writeExactMatch(method: Method, paramValues: Array<Any?>?) {
        val methodParams = (paramValues ?: emptyArray()).zip(method.parameters)
                .map { (argValue, methodParam) ->
                    when {
                        Utils.isHttpBody(methodParam) -> Recorder.MethodParam(Recorder.ParamProcessingWay.BODY, null)
                        else -> Recorder.MethodParam(Recorder.ParamProcessingWay.PARAM, argValue.toString())
                    }
                }
        write(method, methodParams)
    }

    fun write(method: Method, methodParams: List<MethodParam>) {
        ResponseIntersectingFilter.addTask { _, response ->
            response as BufferingFilter.ResponseWrapper
            val request = YamlMethodStub.Request(
                    headerParameters = emptyList(),
                    methodParameters = methodParams.map {
                        YamlMethodStub.RequestParameter(
                                matchType = when (it.paramProcessingWay) {
                                    ParamProcessingWay.BODY -> YamlMethodStub.RequestParameter.MatchType.BODY
                                    ParamProcessingWay.PARAM -> YamlMethodStub.RequestParameter.MatchType.EXACT
                                    ParamProcessingWay.IGNORE -> YamlMethodStub.RequestParameter.MatchType.ANY
                                },
                                value = when (it.paramProcessingWay) {
                                    ParamProcessingWay.PARAM, ParamProcessingWay.IGNORE -> it.value
                                    ParamProcessingWay.BODY -> bodyAsString(ContextSaveFilter.request)
                                }
                        )
                    })

            val yamlResponse = YamlMethodStub.Response(
                    code = response.status,
                    headers = null,
                    body = getResponseDecoder(response).decodeToString(response.getResponseBytes())
            )

            val previousRecords = recordedCalls[method]
            if (previousRecords != mergeRecords(method, request, yamlResponse)) {
                saveRecords(method, recordedCalls[method] ?: emptyMap())
            }
        }
    }

    private fun getResponseDecoder(response: BufferingFilter.ResponseWrapper) =
            responseDecoders[response.getHeader("Content-Encoding")] ?: ResponseDecoder.NoEncodingDecoder

    private fun mergeRecords(method: Method, request: YamlMethodStub.Request, response: YamlMethodStub.Response):
            Map<YamlMethodStub.Request, YamlMethodStub.Response>? {
        return recordedCalls.merge(method, mapOf(request to response)) { cases, _ ->
            if (cases.containsKey(request) && cases[request] != response) {
                println("Overriding recorded response for $request from ${cases[request]} to $response")
            }
            cases + (request to response)
        }
    }

    private fun saveRecords(method: Method, map: Map<YamlMethodStub.Request, YamlMethodStub.Response>) {
        val methodStubsHolder = MethodStubsHolder(listOf(
                YamlMethodStub(
                        className = method.declaringClass.name,
                        methodName = method.name,
                        cases = map.entries.map { entry -> YamlMethodStub.Case(entry.key, entry.value) })))

        recordSaver.saveRecords(methodStubsHolder)
    }

    data class MethodParam(val paramProcessingWay: ParamProcessingWay, val value: String?)

    enum class ParamProcessingWay {
        BODY, PARAM, IGNORE
    }
}