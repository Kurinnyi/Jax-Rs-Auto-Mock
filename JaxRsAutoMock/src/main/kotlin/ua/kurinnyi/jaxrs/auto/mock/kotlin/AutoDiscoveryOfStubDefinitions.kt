package ua.kurinnyi.jaxrs.auto.mock.kotlin

import org.reflections.Reflections

class AutoDiscoveryOfStubDefinitions(private val reflections: Reflections){

    fun getStubDefinitions(): List<StubsDefinition> {
        return reflections.getSubTypesOf(StubsDefinition::class.java)
                .asSequence()
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
}
