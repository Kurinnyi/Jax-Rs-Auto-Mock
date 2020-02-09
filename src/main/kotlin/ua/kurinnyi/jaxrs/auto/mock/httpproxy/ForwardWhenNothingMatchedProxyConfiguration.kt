package ua.kurinnyi.jaxrs.auto.mock.httpproxy

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
