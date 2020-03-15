package ua.kurinnyi.jaxrs.auto.mock.extensions

import ua.kurinnyi.jaxrs.auto.mock.extensions.defaul.ByPackageContextPathConfiguration
/**
 * This interface is used to override context path for the resources.
 * By default all resource are resolve to root context path '/'.
 * The configuration is used only once for each resource interface on the start of the server.
 * Use [ByPackageContextPathConfiguration] as ready to use configuration implementation. Or implement your own.
 */
interface ContextPathsConfiguration {
    /**
     * This method should return context path string for requested resource. Or null if the default one should be used.
     * @param resource - resource class.
     * @return context path for the class or null if it should use default one.
     */
    fun getContextPathsForResource(resource: Class<*>): String?
}