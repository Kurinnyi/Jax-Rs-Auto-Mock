package ua.kurinnyi.jaxrs.auto.mock.kotlin

import ua.kurinnyi.jaxrs.auto.mock.MethodStubsLoader
import ua.kurinnyi.jaxrs.auto.mock.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration

class KotlinMethodStubsLoader(stubDefinitions: List<StubsDefinition>,  proxyConfiguration: ProxyConfiguration) : MethodStubsLoader {

    private val stubs: List<ResourceMethodStub> = stubDefinitions.flatMap { it.getStubs(StubDefinitionContext(proxyConfiguration)) }

    override fun getStubs(): List<ResourceMethodStub> {
        return stubs
    }
}