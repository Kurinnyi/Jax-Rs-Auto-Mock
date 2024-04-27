package ua.kurinnyi.jaxrs.auto.mock.jersey

import io.github.classgraph.ClassGraph
import jakarta.ws.rs.Path
import org.slf4j.LoggerFactory


class AutoDiscoveryOfResourceInterfaces(private val ignoredResources: Set<Class<*>>) {
    private val logger = LoggerFactory.getLogger(AutoDiscoveryOfResourceInterfaces::class.java)
    fun getInterfacesToMock(): List<Class<*>> {
        return ClassGraph()
                .enableAllInfo()
                .scan().use { scanResult ->
                    scanResult.getClassesWithAnnotation(Path::class.java)
                            .map { it.loadClass() }
                }
                .onEach { logger.info("Found resource with path annotation {}", it.name) }
                .filter { it.isInterface }
                .filter { !ignoredResources.contains(it) }
    }

}