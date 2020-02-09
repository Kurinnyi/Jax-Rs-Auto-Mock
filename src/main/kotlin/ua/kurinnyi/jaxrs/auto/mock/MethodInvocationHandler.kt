package ua.kurinnyi.jaxrs.auto.mock

import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.RequestProxy
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubDefinitionsExecutor
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.MethodMock
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.*

class MethodInvocationHandler(
        private val stubDefinitionsExecutor: StubDefinitionsExecutor,
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
                findMatchingMock(method, args, proxy).produceResponse(method, args, dependenciesRegistry)
            }
        }
    }

    private fun findMatchingMock(method: Method, args: Array<Any?>?, proxy: Any): MethodMock {
        val mocks = stubDefinitionsExecutor.getMocks()
        val interfaceName = getInterfaceName(proxy)
        val matchingMock = mocks.firstOrNull { it.isMatchingMethod(method, args, dependenciesRegistry) }
        if (proxyConfiguration.shouldClassBeProxied(interfaceName, matchingMock != null)){
            if (proxyConfiguration.shouldRecord(interfaceName)) {
                dependenciesRegistry.recorder().writeExactMatch(method, args)
            }
            return ProxyingMethodMock(proxyConfiguration.getProxyUrl(interfaceName), dependenciesRegistry.requestProxy())
        }
        verifyClassMocked(mocks, interfaceName)
        return matchingMock ?: throw MockNotFoundException("No mocks matches request: $interfaceName.${method.name}(${Arrays.toString(args)})")
    }

    private class ProxyingMethodMock(private val proxyPath: String, private val requestProxy: RequestProxy) : MethodMock {
        override fun isMatchingMethod(method: Method, receivedArguments: Array<Any?>?, dependenciesRegistry: DependenciesRegistry): Boolean = true

        override fun produceResponse(method: Method, receivedArguments: Array<Any?>?, dependenciesRegistry: DependenciesRegistry): Any? {
            requestProxy.forwardRequest(proxyPath)
            return null
        }

        override fun getMockedClassName(): String = ""

    }

    private fun verifyClassMocked(mocks: List<MethodMock>, interfaceName: String?) {
        mocks.find { it.getMockedClassName() == interfaceName }
                ?: throw MockNotFoundException("Class $interfaceName is present in classpath for mocking. But no stub definition is defined for it")
    }

    private fun getInterfaceName(proxy: Any) = proxy.javaClass.interfaces.first().name

}