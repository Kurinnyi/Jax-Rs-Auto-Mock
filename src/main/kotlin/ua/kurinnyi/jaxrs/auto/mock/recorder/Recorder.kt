package ua.kurinnyi.jaxrs.auto.mock.recorder

import ua.kurinnyi.jaxrs.auto.mock.PlatformUtils
import ua.kurinnyi.jaxrs.auto.mock.Utils.bodyAsString
import ua.kurinnyi.jaxrs.auto.mock.filters.BufferingResponseWrapper
import ua.kurinnyi.jaxrs.auto.mock.filters.HttpRequestResponseHolder
import ua.kurinnyi.jaxrs.auto.mock.filters.ResponseIntersectingFilter
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMocksHolder
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMethodMock
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableObjectMapper
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class Recorder(
        decoderHttps: List<HttpResponseDecoder>,
        private val recordSaver: RecordSaver,
        private val httpRequestResponseHolder: HttpRequestResponseHolder,
        private val responseIntersectingFilter: ResponseIntersectingFilter,
        private val platformUtils: PlatformUtils,
        private val serializableObjectMapper: SerializableObjectMapper
) {

    private val responseDecoders = decoderHttps.flatMap { it.encodings().map { encoding -> encoding to it } }.toMap()

    private val recordedCalls = ConcurrentHashMap<Method, Map<SerializableMethodMock.Request, SerializableMethodMock.Response>>()

    fun writeExactMatch(method: Method, paramValues: Array<Any?>?) {
        val methodParams = (paramValues ?: emptyArray()).zip(method.parameters)
                .map { (argValue, methodParam) ->
                    when {
                        platformUtils.isHttpBody(methodParam) -> Recorder.MethodParam(Recorder.ParamProcessingWay.BODY, null)
                        else -> Recorder.MethodParam(Recorder.ParamProcessingWay.PARAM, argValue.toString())
                    }
                }
        write(method, methodParams)
    }

    fun write(method: Method, methodParams: List<MethodParam>) {
        responseIntersectingFilter.addTask { _, response ->
            response as BufferingResponseWrapper
            val request = SerializableMethodMock.Request(
                    headerParameters = emptyList(),
                    methodParameters = methodParams.map {
                        SerializableMethodMock.RequestParameter(
                                matchType = when (it.paramProcessingWay) {
                                    ParamProcessingWay.BODY -> SerializableMethodMock.RequestParameter.MatchType.BODY
                                    ParamProcessingWay.PARAM -> SerializableMethodMock.RequestParameter.MatchType.EXACT
                                    ParamProcessingWay.IGNORE -> SerializableMethodMock.RequestParameter.MatchType.ANY
                                },
                                value = when (it.paramProcessingWay) {
                                    ParamProcessingWay.PARAM, ParamProcessingWay.IGNORE -> it.value
                                    ParamProcessingWay.BODY -> bodyAsString(httpRequestResponseHolder.request)
                                }
                        )
                    })

            val serializableResponse = SerializableMethodMock.Response(
                    code = response.status,
                    headers = getHeaders(response),
                    body = getResponseDecoder(response).decodeToString(response.getResponseBytes())
            )

            val previousRecords = recordedCalls[method]
            if (previousRecords != mergeRecords(method, request, serializableResponse)) {
                saveRecords(method, recordedCalls[method] ?: emptyMap())
            }
        }
    }

    private fun getHeaders(response: BufferingResponseWrapper): List<SerializableMethodMock.Header>? =
        response.headerNames
                .filterNot { it.toLowerCase() in setOf("content-length", "transfer-encoding") }
                .map { SerializableMethodMock.Header(it, response.getHeader(it))}

    private fun getResponseDecoder(response: BufferingResponseWrapper) =
            responseDecoders[response.getHeader("Content-Encoding")] ?: HttpResponseDecoder.NoEncodingDecoder

    private fun mergeRecords(method: Method, request: SerializableMethodMock.Request, response: SerializableMethodMock.Response):
            Map<SerializableMethodMock.Request, SerializableMethodMock.Response>? {
        return recordedCalls.merge(method, mapOf(request to response)) { cases, _ ->
            if (cases.containsKey(request) && cases[request] != response) {
                println("Overriding recorded response for $request from ${cases[request]} to $response")
            }
            cases + (request to response)
        }
    }

    private fun saveRecords(method: Method, map: Map<SerializableMethodMock.Request, SerializableMethodMock.Response>) {
        val mocksHolder = SerializableMocksHolder(listOf(
                SerializableMethodMock(
                        className = method.declaringClass.name,
                        methodName = method.name,
                        cases = map.entries.map { entry -> SerializableMethodMock.Case(entry.key, entry.value) })))

        recordSaver.saveRecords(mocksHolder, serializableObjectMapper)
    }

    data class MethodParam(val paramProcessingWay: ParamProcessingWay, val value: String?)

    enum class ParamProcessingWay {
        BODY, PARAM, IGNORE
    }
}