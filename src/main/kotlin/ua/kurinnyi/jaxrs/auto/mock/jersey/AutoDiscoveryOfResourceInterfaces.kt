package ua.kurinnyi.jaxrs.auto.mock.jersey

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.reflections.Reflections
import java.io.InputStream
import javax.ws.rs.Path
import kotlin.reflect.KClass

class AutoDiscoveryOfResourceInterfaces(private val reflections: Reflections, private val ignoredResources: Set<KClass<*>>) {

    private val configFileName = "/config/contextPathsConfig.yaml"
    private val defaultContextPath = "/"

    fun getResourceInterfacesToContextMapping(): Map<String, List<Class<*>>> {
        val contextPathsConfig: Map<String, String> = loadContextPathsConfig()
        return getInterfacesToMock().groupBy { i ->
            contextPathsConfig.entries.find { (key, _) ->
                i.name.startsWith(key)
            }?.value ?: defaultContextPath
        }
    }

    private fun getInterfacesToMock(): List<Class<*>> {
        val ignoredResourcesJava = ignoredResources.map { it.java }
        return reflections.getTypesAnnotatedWith(Path::class.java)
                .filter { it.isInterface }
                .filter { !ignoredResourcesJava.contains(it)  }
    }

    private fun loadContextPathsConfig():Map<String,String>{
        val yamlObjectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())
        val contextPathsConfig: InputStream? = this::class.java.getResourceAsStream(configFileName)
        return contextPathsConfig?.let {
            yamlObjectMapper.readValue(it, Map::class.java) as Map<String, String>
        }?: emptyMap()
    }
}