package ua.kurinnyi.jaxrs.auto.mock.serializable

interface SerialisedMocksFilesLoader {
    fun reloadFilesAsStrings():List<String>
}