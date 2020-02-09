package ua.kurinnyi.jaxrs.auto.mock

import ua.kurinnyi.jaxrs.auto.mock.body.ResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.filters.HttpRequestResponseHolder
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.RequestProxy
import ua.kurinnyi.jaxrs.auto.mock.mocks.SerialisationUtils
import ua.kurinnyi.jaxrs.auto.mock.recorder.Recorder

interface DependenciesRegistry {
    fun recorder():Recorder
    fun groupSwitchService():GroupSwitchService
    fun serialisationUtils(): SerialisationUtils
    fun requestProxy(): RequestProxy
    fun httpRequestResponseHolder(): HttpRequestResponseHolder
    fun responseBodyProvider(): ResponseBodyProvider
    fun platformUtils(): PlatformUtils
}