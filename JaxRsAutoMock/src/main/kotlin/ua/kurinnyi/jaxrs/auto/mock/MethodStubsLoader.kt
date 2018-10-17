package ua.kurinnyi.jaxrs.auto.mock

interface MethodStubsLoader {
    fun getStubs(): List<ResourceMethodStub>
}