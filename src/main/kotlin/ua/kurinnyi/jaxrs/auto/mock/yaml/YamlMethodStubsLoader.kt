package ua.kurinnyi.jaxrs.auto.mock.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import ua.kurinnyi.jaxrs.auto.mock.model.StubsGroup
import ua.kurinnyi.jaxrs.auto.mock.MethodStubsLoader
import ua.kurinnyi.jaxrs.auto.mock.model.ResourceMethodStub
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class YamlMethodStubsLoader : MethodStubsLoader {
    override fun getGroups(): List<StubsGroup> = emptyList()
    
    private var responses: List<ResourceMethodStub>

    val yamlObjectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())
    init {
        responses = getMethodStubResponses()
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate({ responses = getMethodStubResponses() }, 0, 10, TimeUnit.SECONDS)
    }

    override fun getStubs(): List<ResourceMethodStub> {
        return responses
    }

    private fun <T> getFolderPath(doWithPath: (Path) -> T) {
        val resource = javaClass.classLoader.getResource("stubs/") ?: return
        val uri = resource.toURI()
        if ("jar" == uri.scheme) {
            FileSystems.newFileSystem(uri, Collections.emptyMap<String, Any>(), null).use {
                doWithPath(it.getPath("stubs/"))
            }
        } else {
            doWithPath(Paths.get(uri))
        }
    }

    private fun getMethodStubResponses(): List<ResourceMethodStub> {
        val list:MutableList<List<YamlMethodStub>> = mutableListOf()
        getFolderPath{ folder ->
            Files.walk(folder).use { files ->
                files.filter { it.toString().toLowerCase().endsWith(".yaml") }
                        .map { readYaml(it)  }
                        .forEach{list.add(it)}
            }
        }
        return  list.flatten().flatMap { it.toFlatStubs() }
    }


    private fun readYaml(path: Path): List<YamlMethodStub> {
        return try {
            Files.newBufferedReader(path).use {
                yamlObjectMapper.readValue(it, MethodStubsHolder::class.java).stubs
            }
        } catch (e: IOException) {
            System.err.println("Failed to load yaml: $path")
            e.printStackTrace()
            emptyList()
        }
    }
}
