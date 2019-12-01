package ua.kurinnyi.jaxrs.auto.mock.recorder

import ua.kurinnyi.jaxrs.auto.mock.Utils.bodyAsString
import ua.kurinnyi.jaxrs.auto.mock.filters.BufferingFilter
import ua.kurinnyi.jaxrs.auto.mock.filters.ContextSaveFilter
import ua.kurinnyi.jaxrs.auto.mock.filters.ResponseIntersectingFilter
import ua.kurinnyi.jaxrs.auto.mock.yaml.YamlMethodStub
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object Recorder {

    private val recordedCalls = ConcurrentHashMap<Method, List<YamlMethodStub.Case>>()

    fun write(method:Method, methodParams: List<MethodParam>) {
        ResponseIntersectingFilter.addTask{ _, response ->
            response as BufferingFilter.ResponseWrapper
            val case = YamlMethodStub.Case(
                    YamlMethodStub.Request(
                            headerParameters = emptyList(),
                            methodParameters = methodParams.map {
                                YamlMethodStub.RequestParameter(
                                        matchType = when (it.paramProcessingWay) {
                                            ParamProcessingWay.BODY -> YamlMethodStub.RequestParameter.MatchType.BODY
                                            ParamProcessingWay.PARAM -> YamlMethodStub.RequestParameter.MatchType.EXACT
                                            ParamProcessingWay.IGNORE -> YamlMethodStub.RequestParameter.MatchType.ANY
                                        },
                                        value =  when (it.paramProcessingWay) {
                                            ParamProcessingWay.PARAM, ParamProcessingWay.IGNORE -> it.value
                                            ParamProcessingWay.BODY -> bodyAsString(ContextSaveFilter.request)
                                        }
                                )
                            }),
                    YamlMethodStub.Response(
                            code = response.status,
                            headers = null,
                            body = String(response.getResponseBytes())
                    )
            )

            recordedCalls.merge(method, listOf(case)) { cases, newCase ->
                cases + newCase
            }
        }
    }

    data class MethodParam(val paramProcessingWay:ParamProcessingWay, val value:String? )

    enum class ParamProcessingWay {
        BODY, PARAM, IGNORE
    }
}