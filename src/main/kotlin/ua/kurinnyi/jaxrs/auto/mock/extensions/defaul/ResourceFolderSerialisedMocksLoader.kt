package ua.kurinnyi.jaxrs.auto.mock.extensions.defaul

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.extensions.SerialisedMocksLoader
import ua.kurinnyi.jaxrs.auto.mock.extensions.SerializableObjectMapper
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMethodMock
import ua.kurinnyi.jaxrs.auto.mock.serializable.SerializableMocksHolder
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * This class is used to load serializable mocks.
 * It search for all files with [filesExtension] extension in resource/mocks folder.
 * And then deserialize them with provided [SerializableObjectMapper].
 * @param filesExtension - types of files to search and read.
 */
class ResourceFolderSerialisedMocksLoader(private val filesExtension: String) : SerialisedMocksLoader {

    override fun reloadMocks(serializableObjectMapper: SerializableObjectMapper): List<SerializableMethodMock> {
        val files = getFolderPath()?.let(::readAllFilesContentInFolder) ?: emptyList()
        return files.flatMap { serializableObjectMapper.read(it, SerializableMocksHolder::class.java).stubs }
    }

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