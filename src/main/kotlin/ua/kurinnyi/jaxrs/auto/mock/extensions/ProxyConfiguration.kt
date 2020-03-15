package ua.kurinnyi.jaxrs.auto.mock.extensions

import ua.kurinnyi.jaxrs.auto.mock.extensions.defaul.ForwardWhenNothingMatchedProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition

/**
 * This interface makes decision whether request should be proxied to external system, and whether request/response
 * should be recorder for further replay.
 * Default implementation is [ForwardWhenNothingMatchedProxyConfiguration].
 */
interface ProxyConfiguration {

    /**
     * This method is indirectly invoked from the [StubsDefinition] implementations, when methods like [bypassAnyNotMatched]
     * invoked. To configure some expected proxy behaviour for the class. In the implementations of this interface it is desired to keep this
     * requests and give them top priority.
     * @param clazzName - the name of the class, which method invocations should be proxied to external system.
     * @param proxyUrl - the path where requests should be forwarded to.
     */
    fun addClassForProxy(clazzName: String, proxyUrl:String?)

    /**
     * This method is indirectly invoked from the [StubsDefinition] implementations, when methods like [recordAnyBypassed]
     * invoked. To configure some expected record behaviour. In the implementations of this interface it is desired to keep this requests and
     * give them top priority.
     * @param clazzName - the name of the class, which responses should be recorded when proxied to external system.
     */
    fun addClassForRecord(clazzName: String)

    /**
     * This method should make decision whether request to the clazz should be proxied.
     * @param clazzName - the name of the class, to make decision about.
     * @param mockIsFound - it says if some mock matching the request is found. In most cases you would not want to proxy requests for which
     * some mock is defined. However sometimes it can be useful to proxy even this requests.
     * @return decision whether request should be proxied.
     */
    fun shouldClassBeProxied(clazzName: String, mockIsFound: Boolean) : Boolean

    /**
     * This method should return the url of external system where the request should be proxied to.
     * @param clazzName - the name of the class, to make decision about.
     * @return url to external system.
     */
    fun getProxyUrl(clazzName: String) : String

    /**
     * This method should make decision whether request/response to the class should be recorded.
     * @param clazzName - the name of the class, to make decision about.
     * @return decision whether request should be recorded.
     */
    fun shouldRecord(clazzName: String) : Boolean
}