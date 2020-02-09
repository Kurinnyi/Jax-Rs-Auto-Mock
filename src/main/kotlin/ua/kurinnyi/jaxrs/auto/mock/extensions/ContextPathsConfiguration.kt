package ua.kurinnyi.jaxrs.auto.mock.extensions

interface ContextPathsConfiguration {
    fun getContextPathsForResource(resource: Class<*>): String?
}