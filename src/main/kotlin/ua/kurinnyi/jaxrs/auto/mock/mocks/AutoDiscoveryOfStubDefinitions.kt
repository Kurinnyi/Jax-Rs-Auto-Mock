package ua.kurinnyi.jaxrs.auto.mock.mocks

import org.reflections.Reflections
import ua.kurinnyi.jaxrs.auto.mock.jersey.groups.GroupConfigurationResourceImpl
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMocksLoader

class AutoDiscoveryOfStubDefinitions(private val reflections: Reflections){

    fun getStubDefinitions(): List<StubsDefinition> {
        return reflections.getSubTypesOf(StubsDefinition::class.java)
                .asSequence()
                .filterNot (::isInternalInstance)
                .map {
                    try {
                        it.getConstructor()
                    } catch (e: NoSuchMethodException) {
                        System.err.println("Class ${it.name} has no empty constructor so you should manually add it. " +
                                "As it would not work with auto discovery")
                        null
                    }
                }.filterNotNull()
                .map { it.newInstance() }
                .toList()
    }

    private fun isInternalInstance(it: Class<out StubsDefinition>) =
            it.name == GroupConfigurationResourceImpl::class.java.name
                    || it.name == SerializableMocksLoader::class.java.name
}
