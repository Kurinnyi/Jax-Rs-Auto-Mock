package ua.kurinnyi.jaxrs.auto.mock.extensions.defaul

import ua.kurinnyi.jaxrs.auto.mock.extensions.ContextPathsConfiguration

open class ByPackageContextPathConfiguration(private val mappings: List<ContextPathMapping>) : ContextPathsConfiguration {

    override fun getContextPathsForResource(resource: Class<*>): String? =
            mappings.firstOrNull() {
                resource.`package`.name.startsWith(it.packageName)
            }?.path
}

data class ContextPathMapping(val packageName: String, val path: String)