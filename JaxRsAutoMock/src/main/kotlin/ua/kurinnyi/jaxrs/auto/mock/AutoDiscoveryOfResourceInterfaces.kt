package ua.kurinnyi.jaxrs.auto.mock

import org.reflections.Reflections
import javax.ws.rs.Path

class AutoDiscoveryOfResourceInterfaces(private val reflections: Reflections) {
    fun getResourceInterfaces(): List<Class<*>> {
        return reflections.getTypesAnnotatedWith(Path::class.java).filter { it.isInterface }
    }
}