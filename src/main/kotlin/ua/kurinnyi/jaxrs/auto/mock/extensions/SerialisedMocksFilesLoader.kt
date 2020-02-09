package ua.kurinnyi.jaxrs.auto.mock.extensions

interface SerialisedMocksFilesLoader {
    fun reloadFilesAsStrings():List<String>
}