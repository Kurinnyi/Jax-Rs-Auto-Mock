package ua.kurinnyi.jaxrs.auto.mock.extensions.defaul

import ua.kurinnyi.jaxrs.auto.mock.extensions.ContextPathsConfiguration

/**
 * This class is used to override context paths for the resources.
 * It receives list of mappings as constructor argument.
 * The [ContextPathMapping.packageName] represent the name of the resource package.
 * Any resource that has package name starting with [ContextPathMapping.packageName] is available on the corresponding [ContextPathMapping.path]
 * For any resource that does not have matching [ContextPathMapping] null is returned.
 * @param mappings - list of context path mappings.
 */
open class ByPackageContextPathConfiguration(private val mappings: List<ContextPathMapping>) : ContextPathsConfiguration {

    override fun getContextPathsForResource(resource: Class<*>): String? =
            mappings.firstOrNull() {
                resource.`package`.name.startsWith(it.packageName)
            }?.path
}

data class ContextPathMapping(val packageName: String, val path: String)