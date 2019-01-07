package ua.kurinnyi.jaxrs.auto.mock

import ua.kurinnyi.jaxrs.auto.mock.model.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.model.StubsGroup

class CompositeMethodStubLoader(private vararg val loaders: MethodStubsLoader) : MethodStubsLoader {
    override fun getGroups(): List<StubsGroup> = loaders.flatMap { it.getGroups() }

    override fun getStubs(): List<ResourceMethodStub> = loaders.flatMap { it.getStubs() }
}