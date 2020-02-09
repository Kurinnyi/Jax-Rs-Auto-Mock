package ua.kurinnyi.jaxrs.auto.mock.jersey

import org.reflections.Reflections
import javax.ws.rs.Path

class AutoDiscoveryOfResourceInterfaces(private val reflections: Reflections, private val ignoredResources: Set<Class<*>>) {

    fun getInterfacesToMock(): List<Class<*>> {
        return reflections.getTypesAnnotatedWith(Path::class.java)
                .filter { it.isInterface }
                .filter { !ignoredResources.contains(it) }
    }

}