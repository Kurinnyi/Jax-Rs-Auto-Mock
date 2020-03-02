package ua.kurinnyi.jaxrs.auto.mock.mocks.apiv2

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.Utils.trimToSingleSpaces
import ua.kurinnyi.jaxrs.auto.mock.extensions.ResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.CompleteMocksData
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ApiAdapterForResponseGeneration
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ExecutableMethodMock
import java.lang.reflect.ParameterizedType

abstract class Mock<Resource : Any>(val definition: Context<Resource>.(Resource) -> Unit) : StubsDefinition {

    private val clazz: Class<Resource> =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<Resource>
    private val context: Context<Resource> = Context(clazz)

    override fun getStubs(): CompleteMocksData {
        definition(context, context.state.instance)
        return context.state.getCompleteMockData()
    }

    override fun getPriority(): Int = context.state.priority

    override fun getGroupsCallbacks(): List<GroupCallback> = context.state.groupCallbacks
}

class Context<Resource>(private val clazz: Class<Resource>) {
    internal val state: ContextState<Resource> = ContextState(clazz)

    fun priority(priority: Int) {
        state.priority = priority
    }

    fun onGroupEnabled(groupName: String, callback: () -> Unit) =
            state.groupCallbacks.add(GroupCallback(groupName, onGroupEnabled = callback))

    fun onGroupDisabled(groupName: String, callback: () -> Unit) =
            state.groupCallbacks.add(GroupCallback(groupName, onGroupDisabled = callback))

    fun bypassAnyNotMatched(path: String) {
        state.proxyConfig = state.proxyConfig.copy(proxyClasses = mapOf(clazz.name to path))
    }

    fun recordAnyBypassed() {
        state.proxyConfig = state.proxyConfig.copy(recordClasses = listOf(clazz.name))
    }

    fun <T> group(name: String, activeByDefault: Boolean = true, body: () -> T): T =
        state.addGroup(activeByDefault, name, body)

    fun <T> group(body: () -> T): T = group("anonymous group", true, body)

    fun <T> T.respond(response: ResponseContext<T>.() -> T) = state.addResponseSection(response)

    operator fun <T> T.invoke(response: ResponseContext<T>.() -> T) = respond(response)

    fun <T> T.header(name: String, value: String?): T {
        state.addRequestHeader(value, name)
        return this
    }

    fun <T> capture(): Captor<T> = Captor(state)

    fun <ARGUMENT> eq(argument: ARGUMENT): ARGUMENT {
        if (argument == null) {
            throw IllegalArgumentException("Please use 'isNull' instead")
        }
        matchNullable<ARGUMENT?> { it == argument }
        return argument
    }

    fun <ARGUMENT> notEq(argument: ARGUMENT): ARGUMENT? {
        if (argument == null) {
            throw IllegalArgumentException("Please use 'notNull' instead")
        }
        matchNullable<ARGUMENT?> { it != argument }
        return argument
    }

    fun <ARGUMENT> notNullAndNotEq(argument: ARGUMENT): ARGUMENT {
        if (argument == null) {
            throw IllegalArgumentException("Please use 'notNull' instead")
        }
        matchNullable<ARGUMENT?> { it != null && it != argument }
        return argument
    }

    fun <ARGUMENT> any(): ARGUMENT? = matchNullable { true }

    fun <ARGUMENT> isNull(): ARGUMENT? = matchNullable { it == null }

    inline fun <reified ARGUMENT> notNull(): ARGUMENT {
        matchNullable<ARGUMENT> { it != null }
        return Utils.getReturnValue(ARGUMENT::class.java)
    }

    inline fun <reified ARGUMENT> match(crossinline predicate: (ARGUMENT) -> Boolean): ARGUMENT {
        matchNullable<ARGUMENT> { it != null && predicate(it) }
        return Utils.getReturnValue(ARGUMENT::class.java)
    }

    fun <ARGUMENT> bodyMatchRegex(regex: String): ARGUMENT? = bodyMatch { regex.toRegex().matches(it) }

    fun <ARGUMENT> bodySameJson(body: String): ARGUMENT? = bodyMatch { trimToSingleSpaces(body) == trimToSingleSpaces(it) }

    inline fun <reified ARGUMENT> anyInRecord(): ARGUMENT {
        val castedPredicate = { _: Any? -> true }
        _addMatcher(ExecutableMethodMock.MatchType.IGNORE_IN_RECORD, castedPredicate)
        return Utils.getReturnValue(ARGUMENT::class.java)
    }

    fun <ARGUMENT> matchNullable(predicate: (ARGUMENT?) -> Boolean): ARGUMENT? {
        val castedPredicate = { arg: Any? -> predicate(arg as ARGUMENT) }
        _addMatcher(ExecutableMethodMock.MatchType.MATCH, castedPredicate)
        return null as ARGUMENT
    }

    fun <ARGUMENT> bodyMatch(predicate: (String) -> Boolean): ARGUMENT? {
        val castedPredicate = { arg: Any? -> predicate(arg as String) }
        _addMatcher(ExecutableMethodMock.MatchType.BODY_MATCH, castedPredicate)
        return null as ARGUMENT
    }

    fun _addMatcher(matchType: ExecutableMethodMock.MatchType, matcher: (Any?) -> Boolean) {
        state.addMatcher(matchType, matcher)
    }
}

class ResponseContext<Response>(
        private val apiAdapter: ApiAdapterForResponseGeneration,
        private val argumentMatchers: List<ExecutableMethodMock.ArgumentMatcher>) {

    fun record() = apiAdapter.recordResponse(argumentMatchers)

    fun code(code: Int): Response? {
        apiAdapter.setResponseCode(code)
        return Utils.getReturnValue(apiAdapter.method)
    }

    fun bodyRaw(body: String): Response? {
        apiAdapter.writeBodyRaw(body)
        return Utils.getReturnValue(apiAdapter.method)
    }

    fun proxyTo(path: String): Response? {
        apiAdapter.proxyTo(path)
        return Utils.getReturnValue(apiAdapter.method)
    }

    fun header(headerName: String, headerValue: String): Response? {
        apiAdapter.setResponseHeader(headerName, headerValue)
        return Utils.getReturnValue(apiAdapter.method)
    }

    fun bodyJson(bodyProvider: ResponseBodyProvider, body: String, vararg templateArgs: Pair<String, Any>): Response =
            apiAdapter.getObjectFromString(bodyProvider, body, templateArgs.toMap())

    fun bodyJson(body: String, vararg templateArgs: Pair<String, Any>): Response =
            apiAdapter.getObjectFromString(body, templateArgs.toMap())
}

class Captor<T>(private val context: ContextState<*>) {
    internal val valueHolder: ThreadLocal<T> = ThreadLocal()

    operator fun invoke(value: T): T {
        context.addCaptor(this as Captor<Any?>)
        return value
    }

    operator fun invoke(): T = valueHolder.get()
}
