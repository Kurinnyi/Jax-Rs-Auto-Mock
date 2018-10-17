package ua.kurinnyi.jaxrs.auto.mock.kotlin

import ua.kurinnyi.jaxrs.auto.mock.MethodStubsLoader
import ua.kurinnyi.jaxrs.auto.mock.ResourceMethodStub

class KotlinMethodStubsLoader(stubDefinitions: List<StubsDefinition>) : MethodStubsLoader {

    private val stubs: List<ResourceMethodStub> = stubDefinitions.flatMap { it.getStubs(StubDefinitionContext()) }

    override fun getStubs(): List<ResourceMethodStub> {
        return stubs
    }
}