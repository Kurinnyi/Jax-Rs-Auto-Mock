package ua.kurinnyi.jaxrs.auto.mock.extensions.defaul

import ua.kurinnyi.jaxrs.auto.mock.extensions.ContextPathsConfiguration

open class ByPackageContextPathConfiguration(private vararg val mappings: ContextPathMapping) : ContextPathsConfiguration {

    override fun getContextPathsForResource(resource: Class<*>): String? =
            mappings.firstOrNull {
                resource.`package`.name.startsWith(it.packageName)
            }?.path


    companion object {
        infix fun String.on(path: String) = ContextPathMapping(this, path)
    }
}

data class ContextPathMapping(val packageName: String, val path: String)