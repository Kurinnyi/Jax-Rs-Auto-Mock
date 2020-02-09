package ua.kurinnyi.jaxrs.auto.mock.serializable

import org.apache.commons.io.IOUtils
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.streams.toList

class ResourceFolderSerialisedMocksFilesLoader(private val filesExtension: String) : SerialisedMocksFilesLoader {

    override fun reloadFilesAsStrings(): List<String> =
            getFolderPath()?.let(::readAllFilesContentInFolder) ?: emptyList()

    private fun getFolderPath(): Path? {
        val resource = javaClass.classLoader.getResource("mocks/") ?: return null
        val uri = resource.toURI()
        return if ("jar" == uri.scheme) {
            FileSystems.newFileSystem(uri, emptyMap<String, Any>(), null).use {
                it.getPath("mocks/")
            }
        } else {
            Paths.get(uri)
        }
    }

    private fun readAllFilesContentInFolder(it: Path) = Files.walk(it).use { files -> readAllFilesContent(files) }

    private fun readAllFilesContent(files: Stream<Path>): List<String> =
            files.filter { it.toString().toLowerCase().endsWith(filesExtension) }
                    .map { IOUtils.toString(Files.newBufferedReader(it)) }
                    .toList()
}