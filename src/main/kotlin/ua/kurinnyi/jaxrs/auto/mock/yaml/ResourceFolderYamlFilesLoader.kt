package ua.kurinnyi.jaxrs.auto.mock.yaml

import org.apache.commons.io.IOUtils
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.streams.toList

object ResourceFolderYamlFilesLoader : YamlFilesLoader {

    override fun reloadYamlFilesAsStrings(): List<String> =
            getFolderPath()?.let(::readAllYamlFilesContentInFolder) ?: emptyList()

    private fun getFolderPath(): Path? {
        val resource = javaClass.classLoader.getResource("stubs/") ?: return null
        val uri = resource.toURI()
        return if ("jar" == uri.scheme) {
            FileSystems.newFileSystem(uri, emptyMap<String, Any>(), null).use {
                it.getPath("stubs/")
            }
        } else {
            Paths.get(uri)
        }
    }

    private fun readAllYamlFilesContentInFolder(it: Path) =
            Files.walk(it).use { files -> readAllYamlFilesContent(files) }

    private fun readAllYamlFilesContent(files: Stream<Path>): List<String> =
            files.filter { it.toString().toLowerCase().endsWith(".yaml") }
                    .map { IOUtils.toString(Files.newBufferedReader(it)) }
                    .toList()
}