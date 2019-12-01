package ua.kurinnyi.jaxrs.auto.mock.httpproxy

open class ExternallyProvidedNothingMatchedProxyConfig(
        private val packageToUrlMapping: Map<String, String>,
        private val packagesToRecord: Set<String>
) : ProxyConfiguration {

    private val nothingMatchedProxyConfiguration = NothingMatchedProxyConfiguration()

    override fun addClassForRecord(clazzName: String) {
        nothingMatchedProxyConfiguration.addClassForRecord(clazzName)
    }

    override fun shouldRecord(clazzName: String): Boolean =
            packagesToRecord.any { clazzName.startsWith(it) }
                    || nothingMatchedProxyConfiguration.shouldRecord(clazzName)

    override fun shouldClassBeProxied(clazzName: String, stubDefinitionIsFound: Boolean): Boolean {
        return (!stubDefinitionIsFound && packageIsConfigured(clazzName))
                || nothingMatchedProxyConfiguration.shouldClassBeProxied(clazzName, stubDefinitionIsFound)
    }

    override fun getProxyUrl(clazzName: String): String =
            if (packageIsConfigured(clazzName)) {
                getProxyUrlForClass(clazzName)
            } else {
                nothingMatchedProxyConfiguration.getProxyUrl(clazzName)
            }

    override fun addClassForProxy(clazzName: String, proxyUrl: String?) {
        if (proxyUrl != null) {
            nothingMatchedProxyConfiguration.addClassForProxy(clazzName, proxyUrl)
        } else {
            getProxyUrlForClass(clazzName)
        }
    }

    private fun packageIsConfigured(clazzName: String) = packageToUrlMapping.keys.any { clazzName.startsWith(it) }

    private fun getProxyUrlForClass(clazzName: String) = packageToUrlMapping.entries.find { clazzName.startsWith(it.key) }?.value
            ?: throw IllegalArgumentException("There is no proxy configuration for $clazzName. Pleas provide it")
}