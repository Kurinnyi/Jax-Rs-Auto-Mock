package ua.kurinnyi.jaxrs.auto.mock

class CompositeMethodStubLoader(private vararg val loaders: MethodStubsLoader) : MethodStubsLoader {

    override fun getStubs(): List<ResourceMethodStub> {
        return loaders.flatMap { it.getStubs() }
    }

}