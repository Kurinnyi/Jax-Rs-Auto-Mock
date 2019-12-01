package ua.kurinnyi.jaxrs.auto.mock.httpproxy

interface ProxyConfiguration {

    fun addClassForProxy(clazzName: String, proxyUrl:String?)

    fun addClassForRecord(clazzName: String)

    fun shouldClassBeProxied(clazzName: String, stubDefinitionIsFound: Boolean) : Boolean

    fun getProxyUrl(clazzName: String) : String

    fun shouldRecord(clazzName: String) : Boolean
}