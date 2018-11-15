package ua.kurinnyi.jaxrs.auto.mock.httpproxy

interface ProxyConfiguration {

    fun addClass(clazzName: String, proxyUrl:String?)

    fun shouldClassBeProxied(clazzName: String, stubDefinitionIsFound: Boolean) : Boolean

    fun getProxyUrl(clazzName: String) : String
}