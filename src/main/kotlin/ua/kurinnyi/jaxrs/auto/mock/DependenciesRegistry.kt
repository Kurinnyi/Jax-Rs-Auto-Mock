package ua.kurinnyi.jaxrs.auto.mock

import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.filters.ContextSaveFilter
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.RequestProxy
import ua.kurinnyi.jaxrs.auto.mock.mocks.SerialisationUtils
import ua.kurinnyi.jaxrs.auto.mock.recorder.Recorder

interface DependenciesRegistry {
    fun recorder():Recorder
    fun groupSwitchService():GroupSwitchService
    fun serialisationUtils(): SerialisationUtils
    fun requestProxy(): RequestProxy
    fun contextSaveFilter(): ContextSaveFilter
    fun bodyProvider(): BodyProvider
    fun platformUtils(): PlatformUtils
}