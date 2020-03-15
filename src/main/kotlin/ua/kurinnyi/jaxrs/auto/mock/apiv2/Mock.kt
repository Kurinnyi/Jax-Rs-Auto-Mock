package ua.kurinnyi.jaxrs.auto.mock.apiv2

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.Utils.trimToSingleSpaces
import ua.kurinnyi.jaxrs.auto.mock.extensions.ResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.CompleteMocksData
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ApiAdapterForResponseGeneration
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ExecutableMethodMock
import ua.kurinnyi.jaxrs.auto.mock.extensions.ProxyConfiguration
import java.lang.reflect.ParameterizedType

/**
 * Extend this class to define some mocks for the Resource
 * @param Resource - the class to be mocked
 * @property definition - the lambda with the definitions of the mocks
 */
abstract class Mock<Resource : Any>(val definition: Context<Resource>.(Resource) -> Unit) : StubsDefinition {

    private val clazz: Class<Resource> =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<Resource>
    private val context: Context<Resource> = Context(clazz)

    /**
     * Executes your mock definitions.
     * Not supposed to be overridden.
     */
    override fun getStubs(): CompleteMocksData {
        definition(context, context.state.instance)
        return context.state.getCompleteMockData()
    }

    /**
     * Defines the priority of mocks.
     * Not supposed to be overridden. Use [Context.priority] method instead.
     */
    override fun getPriority(): Int = context.state.priority

    /**
     * Defines the groups callbacks.
     * Not supposed to be overridden. Use [Context.onGroupEnabled], [Context.onGroupEnabled] methods instead.
     */
    override fun getGroupsCallbacks(): List<GroupCallback> = context.state.groupCallbacks
}

/**
 * The instance of the class is provided to you to let build mocks.
 * It contains all required methods to define any kind of behaviour.
 * It should not be instantiated directly.
 */
class Context<Resource>(private val clazz: Class<Resource>) {
    internal val state: ContextState<Resource> = ContextState(clazz)

    /**
     * Sets the priority of the mocks to ensure ordering.
     * Makes sense to use only if mocks for same resource defined in different places.
     */
    fun priority(priority: Int) {
        state.priority = priority
    }

    /**
     * If invoked any request to resource for which mock is not found, is forwarded to external system.
     * Invokes method [ProxyConfiguration.addClassForProxy] under the hood.
     * @param path - url of external system, where request should be forwarded.
     */
    fun bypassAnyNotMatched(path: String) {
        state.proxyConfig = state.proxyConfig.copy(proxyClasses = mapOf(clazz.name to path))
    }

    /**
     * If invoked any request/response that is forwarded to external system, is recorded
     * Invokes method [ProxyConfiguration.addClassForRecord] under the hood.
     */
    fun recordAnyBypassed() {
        state.proxyConfig = state.proxyConfig.copy(recordClasses = listOf(clazz.name))
    }

    /**
     * Matcher method. Mock matches if request parameter is equal to provided value.
     */
    fun <ARGUMENT> eq(argument: ARGUMENT): ARGUMENT {
        if (argument == null) {
            throw IllegalArgumentException("Please use 'isNull' instead")
        }
        matchNullable<ARGUMENT?> { it == argument }
        return argument
    }

    /**
     * Matcher method. Mock matches if request parameter is not equal to provided value.
     * It also matches if request parameter is null.
     */
    fun <ARGUMENT> notEq(argument: ARGUMENT): ARGUMENT? {
        if (argument == null) {
            throw IllegalArgumentException("Please use 'notNull' instead")
        }
        matchNullable<ARGUMENT?> { it != argument }
        return argument
    }

    /**
     * Matcher method. Mock matches if request parameter is not equal to provided value
     * and is not null.
     */
    fun <ARGUMENT> notNullAndNotEq(argument: ARGUMENT): ARGUMENT {
        if (argument == null) {
            throw IllegalArgumentException("Please use 'notNull' instead")
        }
        matchNullable<ARGUMENT?> { it != null && it != argument }
        return argument
    }

    /**
     * Matcher method. Mock matches any request parameter including null.
     */
    fun <ARGUMENT> any(): ARGUMENT? = matchNullable { true }

    /**
     * Matcher method. Mock matches only when request parameter is null.
     */
    fun <ARGUMENT> isNull(): ARGUMENT? = matchNullable { it == null }

    /**
     * Matcher method. Mock matches any request parameter except null.
     */
    inline fun <reified ARGUMENT> notNull(): ARGUMENT {
        matchNullable<ARGUMENT> { it != null }
        return Utils.getReturnValue(ARGUMENT::class.java)
    }

    /**
     * Matcher method. Mock matches if request parameter is not null and matches provided predicate.
     */
    inline fun <reified ARGUMENT> match(crossinline predicate: (ARGUMENT) -> Boolean): ARGUMENT {
        matchNullable<ARGUMENT> { it != null && predicate(it) }
        return Utils.getReturnValue(ARGUMENT::class.java)
    }

    /**
     * Matcher method. Mock matches if request parameter matches provided predicate.
     */
    fun <ARGUMENT> matchNullable(predicate: (ARGUMENT?) -> Boolean): ARGUMENT? {
        val castedPredicate = { arg: Any? -> predicate(arg as ARGUMENT) }
        _addMatcher(ExecutableMethodMock.MatchType.MATCH, castedPredicate)
        return null as ARGUMENT
    }

    /**
     * Matcher method. Mock matches any request parameter including null.
     * If the request/response is recorded, parameter for this matcher in record will match any.
     * Not recommended to use this method with [Captor] as it may lead to nullability issues.
     */
    inline fun <reified ARGUMENT> anyInRecord(): ARGUMENT {
        val castedPredicate = { _: Any? -> true }
        _addMatcher(ExecutableMethodMock.MatchType.IGNORE_IN_RECORD, castedPredicate)
        return Utils.getReturnValue(ARGUMENT::class.java)
    }

    /**
     * Matcher method. It matches against raw incoming http request body.
     * Mock matches when http body matches regex.
     */
    fun <ARGUMENT> bodyMatchRegex(regex: String): ARGUMENT? = bodyMatch { regex.toRegex().matches(it) }

    /**
     * Matcher method. It matches against raw incoming http request body.
     * Mock matches when http body is equal to provided body. Indentations on both sides are ignored.
     */
    fun <ARGUMENT> bodySame(body: String): ARGUMENT? = bodyMatch { trimToSingleSpaces(body) == trimToSingleSpaces(it) }

    /**
     * Matcher method. It matches against raw incoming http request body.
     * Mock matches when http body matches predicate.
     */
    fun <ARGUMENT> bodyMatch(predicate: (String) -> Boolean): ARGUMENT? {
        val castedPredicate = { arg: Any? -> predicate(arg as String) }
        _addMatcher(ExecutableMethodMock.MatchType.BODY_MATCH, castedPredicate)
        return null as ARGUMENT
    }

    /**
     * This method adds additional restriction on mock matching.
     * Incoming http request should have header with matching name and value.
     * You can use matcher like [Context.eq], [Context.match] to match the value of the header,
     * or just provide expected value.
     */
    fun <T> T.header(name: String, value: String?): T {
        state.addRequestHeader(value, name)
        return this
    }

    /**
     * Creates new [Captor] that can be used for accessing incoming request parameters
     * from response generation section.
     */
    fun <T> capture(): Captor<T> = Captor(state)

    /**
     * Groups the set of the mocks. All the mocks in the group can be activated/deactivated
     * by calls to [GroupConfigurationResource].
     * @param name - the name of the group. It can be controlled by this name.
     * @param activeByDefault - indicates whether mocks inside group are activated at launch.
     * @param body - the lambda with the definitions of the mocks of this group.
     */
    fun <T> group(name: String, activeByDefault: Boolean = true, body: () -> T): T =
            state.addGroup(activeByDefault, name, body)

    /**
     * Groups the set of the mocks. Can be used for some logical organisation of mocks code.
     * @param body - the lambda with the definitions of the mocks of this group.
     */
    fun <T> group(body: () -> T): T = group("anonymous group", true, body)

    /**
     * Sets the callback to be executed once group is turned on.
     * Makes sense to use to do some clean up or something like this in stateful mocks.
     * @param groupName - name of the group to track toggles.
     * @param callback - callback to be executed once group is switched on.
     */
    fun onGroupEnabled(groupName: String, callback: () -> Unit) =
            state.groupCallbacks.add(GroupCallback(groupName, onGroupEnabled = callback))

    /**
     * Sets the callback to be executed once group is turned off.
     * Makes sense to use to do some clean up or something like this in stateful mocks.
     * @param groupName - name of the group to track toggles.
     * @param callback - callback to be executed once group is switched off.
     */
    fun onGroupDisabled(groupName: String, callback: () -> Unit) =
            state.groupCallbacks.add(GroupCallback(groupName, onGroupDisabled = callback))

    /**
     * Once request matchers are defined, invoke this method to configure the way to response.
     * @param response - the lambda with the definition of the response
     */
    fun <T> T.respond(response: ResponseContext<T>.() -> T) = state.addResponseSection(response)

    /**
     * Same as [Mock.respond]  but a bit more concise
     * @param response - the lambda with the definition of the response
     */
    operator fun <T> T.invoke(response: ResponseContext<T>.() -> T) = respond(response)

    /**
     * Please do not use this method. It is public only cause of technical limitations.
     */
    fun _addMatcher(matchType: ExecutableMethodMock.MatchType, matcher: (Any?) -> Boolean) {
        state.addMatcher(matchType, matcher)
    }
}

/**
 * The instance of the class is provided to you to let build response for the mocks.
 * It contains all required methods to create responses in any way.
 * It should not be instantiated directly.
 */
class ResponseContext<Response>(
        private val apiAdapter: ApiAdapterForResponseGeneration,
        private val argumentMatchers: List<ExecutableMethodMock.ArgumentMatcher>) {

    /**
     * Specifies the http response code.
     * Normally you should not invoke this method,
     * but rely on the mechanisms of the platform to resolve correct status code.
     */
    fun code(code: Int): Response? {
        apiAdapter.setResponseCode(code)
        return Utils.getReturnValue(apiAdapter.method)
    }

    /**
     * Specifies the http response header.
     * Normally you should not invoke this method for standard headers like Content-Type or Content-Length,
     * but rely on the mechanisms of the platform to resolve them.
     * It makes sense to use it to specify some custom headers.
     */
    fun header(headerName: String, headerValue: String): Response? {
        apiAdapter.setResponseHeader(headerName, headerValue)
        return Utils.getReturnValue(apiAdapter.method)
    }

    /**
     * Writes the provided body as the response directly.
     * Please consider using [ResponseContext.body] methods instead, as this one does not ensure any type safety.
     * It makes sense to use it with [ResponseContext.code] to provide some unusual errors.
     */
    fun bodyRaw(body: String): Response? {
        apiAdapter.writeBodyRaw(body)
        return Utils.getReturnValue(apiAdapter.method)
    }

    /**
     * Uses provided body as template or as the path to template file, then
     * it puts arguments into the template if provided, and generates instance of Response.
     * You can return the response object directly or do something with it.
     * It uses default [ResponseBodyProvider] to create object out of the body string.
     * @param body - Response object serialized as string or path to the file with such information.
     * @param templateArgs - argument to be passed to the template, before deserializing.
     */
    fun body(body: String, vararg templateArgs: Pair<String, Any>): Response =
            apiAdapter.getObjectFromString(body, templateArgs.toMap())

    /**
     * Uses provided body as template or as the path to template file, then
     * it puts arguments into the template if provided, and generates instance of Response.
     * You can return the response object directly or do something with it.
     * @param bodyProvider -  object that decides how to create Response object from the string.
     * @param body - Response object serialized as string or path to the file with such information.
     * @param templateArgs - argument to be passed to the template, before deserializing.
     */
    fun body(bodyProvider: ResponseBodyProvider, body: String, vararg templateArgs: Pair<String, Any>): Response =
            apiAdapter.getObjectFromString(bodyProvider, body, templateArgs.toMap())

    /**
     * When invoked, request/response is forwarded to the external url.
     * @param path - external url.
     */
    fun proxyTo(path: String): Response? {
        apiAdapter.proxyTo(path)
        return Utils.getReturnValue(apiAdapter.method)
    }

    /**
     * When invoked, request/response is recorded as serialized mock.
     */
    fun record() = apiAdapter.recordResponse(argumentMatchers)
}

/**
 * Keeps the request parameter, so it can be accessed from response generation section.
 * It should not be instantiated directly.
 */
class Captor<T>(private val context: ContextState<*>) {
    internal val valueHolder: ThreadLocal<T> = ThreadLocal()

    /**
     * Wrap a matcher method with this, to capture the value of the matching parameter.
     */
    operator fun invoke(value: T): T {
        context.addCaptor(this as Captor<Any?>)
        return value
    }

    /**
     * Returns the captured value. Should be used only in response generation section.
     */
    operator fun invoke(): T = valueHolder.get()
}
