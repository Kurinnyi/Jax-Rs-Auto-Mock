package ua.kurinnyi.jaxrs.auto.mock

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.*

class MethodInvocationHandler(private val methodStubsLoader: MethodStubsLoader) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? =
            when (method.name) {
                "hashCode" -> proxy.javaClass.name.hashCode()
                "equals" -> proxy === args!![0]
                "toString" -> getInterfaceName(proxy)
                else -> findMatchingStub(method, args, proxy)
                        .produceResponse(method, ContextSaveFilter.response)
            }

    private fun findMatchingStub(method: Method, args: Array<Any>?, proxy: Any): ResourceMethodStub {
        val stubs = methodStubsLoader.getStubs()
        val interfaceName = getInterfaceName(proxy)
        checkIfClassIsStubbed(stubs, interfaceName)
        return stubs.firstOrNull {
            it.isMatchingMethod(method, args, ContextSaveFilter.request)
        } ?: throw StubNotFoundException("No stubs matches request: $interfaceName.${method.name}(${Arrays.toString(args)})")
    }

    private fun checkIfClassIsStubbed(stubs: List<ResourceMethodStub>, interfaceName: String?) {
        stubs.find { it.getStubbedClassName() == interfaceName }
                ?: throw StubNotFoundException("Class $interfaceName is present in classpath for stubbing. " +
                        "But no stub definition is defined for it")
    }

    private fun getInterfaceName(proxy: Any) = proxy.javaClass.interfaces.first().name

}