package ua.kurinnyi.jaxrs.auto.mock

import java.lang.reflect.Proxy

class ProxyInstanceFactory(private val methodInvocationHandler: MethodInvocationHandler) {
    fun <T> createMockInstanceForInterface(interfaceToMock: Class<T>): T {
        return Proxy.newProxyInstance(interfaceToMock.classLoader, arrayOf(interfaceToMock), methodInvocationHandler) as T
    }
}