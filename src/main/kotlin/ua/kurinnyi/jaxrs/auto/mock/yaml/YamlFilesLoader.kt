package ua.kurinnyi.jaxrs.auto.mock.yaml

interface YamlFilesLoader {
    fun reloadYamlFilesAsStrings():List<String>
}