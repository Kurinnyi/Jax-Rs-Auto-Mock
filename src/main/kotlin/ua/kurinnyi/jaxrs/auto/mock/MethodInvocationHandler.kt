package ua.kurinnyi.jaxrs.auto.mock

import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.RequestProxy
import ua.kurinnyi.jaxrs.auto.mock.mocks.MethodStubsLoader
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.ResourceMethodStub
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.*

class MethodInvocationHandler(
        private val methodStubsLoader: MethodStubsLoader,
        private val proxyConfiguration: ProxyConfiguration,
        private val dependenciesRegistry: DependenciesRegistry
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        val interfaceName = getInterfaceName(proxy)
        return when (method.name) {
            "hashCode" -> proxy.javaClass.name.hashCode()
            "equals" -> proxy === args!![0]
            "toString" -> interfaceName
            else -> {
                findMatchingStub(method, args, proxy).produceResponse(method, args, dependenciesRegistry)
            }
        }
    }

    private fun findMatchingStub(method: Method, args: Array<Any?>?, proxy: Any): ResourceMethodStub {
        val stubs = methodStubsLoader.getStubs()
        val interfaceName = getInterfaceName(proxy)
        val matchingStub = stubs.firstOrNull { it.isMatchingMethod(method, args, dependenciesRegistry) }
        if (proxyConfiguration.shouldClassBeProxied(interfaceName, matchingStub != null)){
            if (proxyConfiguration.shouldRecord(interfaceName)) {
                dependenciesRegistry.recorder().writeExactMatch(method, args)
            }
            return ProxyingResourceMethodStub(proxyConfiguration.getProxyUrl(interfaceName), dependenciesRegistry.requestProxy())
        }
        verifyClassStubbed(stubs, interfaceName)
        return matchingStub ?: throw StubNotFoundException("No stubs matches request: $interfaceName.${method.name}(${Arrays.toString(args)})")
    }

    private class ProxyingResourceMethodStub(private val proxyPath: String, private val requestProxy: RequestProxy) : ResourceMethodStub {
        override fun isMatchingMethod(method: Method, args: Array<Any?>?, dependenciesRegistry: DependenciesRegistry): Boolean = true

        override fun produceResponse(method: Method, args: Array<Any?>?, dependenciesRegistry: DependenciesRegistry): Any? {
            requestProxy.forwardRequest(proxyPath)
            return null
        }

        override fun getStubbedClassName(): String = ""

    }

    private fun verifyClassStubbed(stubs: List<ResourceMethodStub>, interfaceName: String?) {
        stubs.find { it.getStubbedClassName() == interfaceName } ?: throw StubNotFoundException("Class $interfaceName is present in classpath for stubbing. " +
                        "But no stub definition is defined for it")
    }

    private fun getInterfaceName(proxy: Any) = proxy.javaClass.interfaces.first().name

}