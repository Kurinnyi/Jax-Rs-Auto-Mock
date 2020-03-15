package ua.kurinnyi.jaxrs.auto.mock.extensions.defaul

import ua.kurinnyi.jaxrs.auto.mock.extensions.ProxyConfiguration

/**
 * This class makes decision whether request should be proxied to external system, and whether request/response
 * should be recorder for further replay.
 * It only proxy request to the classes that was specified in [addClassForProxy] method and only when no mocks found for the request.
 * It only records requests to the classes that was specified in [addClassForRecord] method.
 */
open class ForwardWhenNothingMatchedProxyConfiguration : ProxyConfiguration {
    private val classesToBeProxied:MutableMap<String, String> = hashMapOf()
    private val classesToBeRecorded:MutableSet<String> = mutableSetOf()

    override fun addClassForProxy(clazzName: String, proxyUrl:String?) {
        classesToBeProxied[clazzName] = (proxyUrl ?:
                throw IllegalArgumentException("You should provide proxyUrl, or override ProxyConfiguration to one that not requires it."))
    }

    override fun addClassForRecord(clazzName: String) {
        classesToBeRecorded.add(clazzName)
    }

    override fun shouldClassBeProxied(clazzName: String, mockIsFound: Boolean) : Boolean  =
            !mockIsFound && classesToBeProxied.containsKey(clazzName)

    override fun getProxyUrl(clazzName: String) : String =
            classesToBeProxied[clazzName]?:throw IllegalArgumentException("Class $clazzName is not configured to be proxied")

    override fun shouldRecord(clazzName: String): Boolean =
        classesToBeRecorded.contains(clazzName)

}
