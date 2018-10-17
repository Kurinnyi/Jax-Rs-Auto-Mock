package ua.kurinnyi.jaxrs.auto.mock

import org.glassfish.jersey.server.ResourceConfig
import java.lang.reflect.Proxy

class ResourceLoaderOfProxyInstances(
        resourceInterfaces: List<Class<*>>,
        private val methodInvocationHandler: MethodInvocationHandler) : ResourceConfig() {

    init {
        registerInstances(resourceInterfaces.map(this::createMockInstanceForInterface).toSet())
    }

    private fun createMockInstanceForInterface(interfaceToMock: Class<*>): Any {
        return Proxy.newProxyInstance(interfaceToMock.classLoader, arrayOf(interfaceToMock), methodInvocationHandler)
    }
}