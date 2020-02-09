package ua.kurinnyi.jaxrs.auto.mock.serializable

interface SerializableFilesLoader {
    fun reloadFilesAsStrings():List<String>
}