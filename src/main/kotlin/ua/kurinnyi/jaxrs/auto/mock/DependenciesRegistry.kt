package ua.kurinnyi.jaxrs.auto.mock

import ua.kurinnyi.jaxrs.auto.mock.extensions.ResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.filters.HttpRequestResponseHolder
import ua.kurinnyi.jaxrs.auto.mock.response.RequestProxy
import ua.kurinnyi.jaxrs.auto.mock.mocks.SerialisationUtils
import ua.kurinnyi.jaxrs.auto.mock.response.Recorder

interface DependenciesRegistry {
    fun recorder(): Recorder
    fun groupSwitchService():GroupSwitchService
    fun serialisationUtils(): SerialisationUtils
    fun requestProxy(): RequestProxy
    fun httpRequestResponseHolder(): HttpRequestResponseHolder
    fun responseBodyProvider(): ResponseBodyProvider
    fun platformUtils(): PlatformUtils
}