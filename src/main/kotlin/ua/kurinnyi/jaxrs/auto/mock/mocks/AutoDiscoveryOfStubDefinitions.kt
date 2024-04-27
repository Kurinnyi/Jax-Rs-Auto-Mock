package ua.kurinnyi.jaxrs.auto.mock.mocks

import io.github.classgraph.ClassGraph
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier

class AutoDiscoveryOfStubDefinitions {
    private val logger = LoggerFactory.getLogger(AutoDiscoveryOfStubDefinitions::class.java)
    fun getStubDefinitions(): List<StubsDefinition> {
        return ClassGraph()
                .enableAllInfo()
                .scan().use { scanResult ->
                    scanResult.getClassesImplementing(StubsDefinition::class.java)
                            .map { it.loadClass() }
                            .map { it as Class<StubsDefinition> }
                }.asSequence()
                .onEach { logger.info("Found stub definition {}", it.name) }
                .filterNot(::isInternalInstance)
                .filterNot(::isAbstract)
                .map {
                    try {
                        it.getConstructor()
                    } catch (e: NoSuchMethodException) {
                        logger.error("Class {} has no empty constructor so you should manually add it. " +
                                "As it would not work with auto discovery", it.name)
                        null
                    }
                }.filterNotNull()
                .map { it.newInstance() }
                .toList()
    }

    private fun isInternalInstance(it: Class<out StubsDefinition>) =
            it.`package`.name.startsWith("ua.kurinnyi.jaxrs.auto.mock.")

    private fun isAbstract(it: Class<out StubsDefinition>) =
            Modifier.isAbstract(it.modifiers)
}
