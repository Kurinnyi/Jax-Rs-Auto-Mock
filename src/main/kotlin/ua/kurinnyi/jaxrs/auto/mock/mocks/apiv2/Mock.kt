package ua.kurinnyi.jaxrs.auto.mock.mocks.apiv2

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.extensions.ResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.CompleteMocksData
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.ProxyConfig
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ApiAdapterForResponseGeneration
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ExecutableMethodMock
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.GroupOfMethodMocks
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy

abstract class Mock<Resource : Any>(val definition: Context<Resource>.(Resource) -> Unit) : StubsDefinition {

    private val clazz: Class<Resource> =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<Resource>
    private val context: Context<Resource> = Context(clazz)

    override fun getStubs(): CompleteMocksData {
        definition(context, context.instance)
        if (context.methodMocksUnderConstruction.isNotEmpty())
            throw IllegalStateException("There are some method mocks without response section")
        return CompleteMocksData(
                context.constructedMethodMocks.map { it.executableMethodMock },
                context.constructedGroups.map { it.group },
                context.proxyConfig)
    }

    override fun getPriority(): Int {
        return context.priority
    }

    override fun getGroupsCallbacks(): List<GroupCallback> {
        return context.groupCallbacks
    }

}

class Context<Resource>(private val clazz: Class<Resource>) {

    internal var priority = 0
    var _tempArgList: MutableList<ExecutableMethodMock.ArgumentMatcher> = mutableListOf()
    private var captors: MutableList<Captor<Any?>?> = mutableListOf()
    internal var methodMocksUnderConstruction: MutableList<MethodMockBuilder> = mutableListOf()
    private var groupsUnderConstruction: MutableList<GroupBuilder> = mutableListOf()

    internal val groupCallbacks: MutableList<GroupCallback> = mutableListOf()
    internal var proxyConfig: ProxyConfig = ProxyConfig(emptyMap(), emptyList())
    internal val constructedMethodMocks: MutableList<MethodMockBuilder> = mutableListOf()
    internal val constructedGroups: MutableList<GroupBuilder> = mutableListOf()

    val instance = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { _, method, args ->
        checkAllArgumentsSetUp(args, method)
        val methodMockBuilder = MethodMockBuilder(method, _tempArgList, captors)
        methodMocksUnderConstruction.add(methodMockBuilder)
        _tempArgList = mutableListOf()
        captors = mutableListOf()
        groupsUnderConstruction.forEach { group -> group.methodMocks.add(methodMockBuilder) }
        methodMockBuilder.isActivatedByGroups = groupsUnderConstruction
                .lastOrNull()?.let { it.status == GroupStatus.ACTIVE } ?: true
        Utils.getReturnValue(method)
    } as Resource

    fun priority(priority: Int) {
        this.priority = priority
    }

    fun onGroupEnabled(groupName: String, callback: () -> Unit) {
        groupCallbacks.add(GroupCallback(groupName, onGroupEnabled = callback))
    }

    fun onGroupDisabled(groupName: String, callback: () -> Unit) {
        groupCallbacks.add(GroupCallback(groupName, onGroupDisabled = callback))
    }

    fun bypassAnyNotMatched(path: String) {
        proxyConfig = proxyConfig.copy(proxyClasses = mapOf(clazz.name to path))
    }

    fun recordAnyBypassed() {
        proxyConfig = proxyConfig.copy(recordClasses = listOf(clazz.name))
    }

    fun <T> group(name: String, activeByDefault: Boolean = true, body: () -> T): T {
        val status = if (activeByDefault) GroupStatus.ACTIVE else GroupStatus.NON_ACTIVE
        val group = GroupBuilder(name, mutableListOf(), status)
        if (groupsUnderConstruction.any { it.name == group.name })
            throw IllegalArgumentException("Group $name contains itself recursively. This is forbidden.")
        groupsUnderConstruction.add(group)
        val result = body()
        constructedGroups.add(group)
        groupsUnderConstruction.remove(group)
        return result
    }

    fun <T> group(body: () -> T): T {
        return group("anonymous group", true, body)
    }

    fun <T> capture(): Captor<T> = Captor(this)

    fun <T> T.respond(response: ResponseContext<T>.() -> T) {
        methodMocksUnderConstruction.forEach {
            val captors = it.captors
            it.responseSection = { apiAdapter, args, methodStub ->
                captors.forEachIndexed { i, captor -> captor?.valueHolder?.set(args[i]) }
                response(ResponseContext(apiAdapter, methodStub.argumentsMatchers))
            }
        }
        checkReturnTypesOfMockedMethods()
        constructedMethodMocks.addAll(methodMocksUnderConstruction)
        methodMocksUnderConstruction = mutableListOf()
    }

    private fun checkReturnTypesOfMockedMethods() {
        methodMocksUnderConstruction
                .asSequence()
                .map { it.method }
                .windowed(2)
                .forEach { (m1, m2) ->
                    if (m1.returnType != m2.returnType)
                        throw IllegalStateException("Trying to have a single response " +
                                "section for methods with different return types: $m1, $m2")
                }
    }

    operator fun <T> T.invoke(response: ResponseContext<T>.() -> T) {
        return respond(response)
    }

    fun <T> T.header(name: String, value: String?): T {
        if (_tempArgList.isEmpty()) {
            eq(value)
        }
        methodMocksUnderConstruction.forEach {
            it.requestHeaders += ExecutableMethodMock.HeaderParameter(name, _tempArgList.last())
        }
        _tempArgList = mutableListOf()
        return this
    }

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

    fun <ARGUMENT> any(): ARGUMENT? {
        return matchNullable { true }
    }

    fun <ARGUMENT> isNull(): ARGUMENT? {
        return matchNullable { it == null }
    }

    inline fun <reified ARGUMENT> notNull(): ARGUMENT {
        matchNullable<ARGUMENT> { it != null }
        return Utils.getReturnValue(ARGUMENT::class.java)
    }

    inline fun <reified ARGUMENT> match(crossinline predicate: (ARGUMENT) -> Boolean): ARGUMENT {
        matchNullable<ARGUMENT> { it != null && predicate(it) }
        return Utils.getReturnValue(ARGUMENT::class.java)
    }

    fun <ARGUMENT> bodyMatchRegex(regex: String): ARGUMENT? =
            bodyMatch { regex.toRegex().matches(it) }

    fun <ARGUMENT> bodySameJson(body: String): ARGUMENT? =
            bodyMatch { Utils.trimToSingleSpaces(body) == Utils.trimToSingleSpaces(it) }

    inline fun <reified ARGUMENT> anyInRecord(): ARGUMENT {
        val castedPredicate = { arg: Any? -> true }
        _tempArgList.add(ExecutableMethodMock.ArgumentMatcher(ExecutableMethodMock.MatchType.IGNORE_IN_RECORD, castedPredicate))
        return Utils.getReturnValue(ARGUMENT::class.java)
    }

    fun <ARGUMENT> matchNullable(predicate: (ARGUMENT?) -> Boolean): ARGUMENT? {
        val castedPredicate = { arg: Any? -> predicate(arg as ARGUMENT) }
        _tempArgList.add(ExecutableMethodMock.ArgumentMatcher(ExecutableMethodMock.MatchType.MATCH, castedPredicate))
        return null as ARGUMENT
    }

    fun <ARGUMENT> bodyMatch(predicate: (String) -> Boolean): ARGUMENT? {
        val castedPredicate = { arg: Any? -> predicate(arg as String) }
        _tempArgList.add(ExecutableMethodMock.ArgumentMatcher(ExecutableMethodMock.MatchType.BODY_MATCH, castedPredicate))
        return null as ARGUMENT
    }

    internal fun addCaptor(captor: Captor<Any?>) {
        if (_tempArgList.isEmpty())
            throw IllegalStateException("Captor is used incorrectly. It should wrap argument matcher like 'captorName(any())'.")
        while (captors.size < _tempArgList.size - 1) {
            captors.add(null)
        }
        captors.add(captor)
    }

    private fun checkAllArgumentsSetUp(args: Array<Any>?, method: Method) {
        if (args != null && _tempArgList.size < args.size) {
            throw IllegalArgumentException("Not all parameters of method: ${method.name} are " +
                    "called with matchers. Use match methods like 'eq','any' etc for all the parameters")
        }
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

class Captor<T>(private val context: Context<*>) {
    val valueHolder: ThreadLocal<T> = ThreadLocal()

    operator fun invoke(value: T): T {
        context.addCaptor(this as Captor<Any?>)
        return value
    }

    operator fun invoke(): T {
        return valueHolder.get()
    }
}

internal data class MethodMockBuilder(
        val method: Method,
        private val arguments: List<ExecutableMethodMock.ArgumentMatcher>,
        val captors: List<Captor<Any?>?>) {
    internal var requestHeaders: List<ExecutableMethodMock.HeaderParameter> = listOf()
    lateinit var responseSection: ((ApiAdapterForResponseGeneration, Array<Any?>, ExecutableMethodMock) -> Any?)
    internal var isActivatedByGroups: Boolean = true

    internal val executableMethodMock: ExecutableMethodMock by lazy {
        ExecutableMethodMock(method, arguments, requestHeaders, responseSection, isActivatedByGroups)
    }
}

internal data class GroupBuilder(val name: String, val methodMocks: MutableList<MethodMockBuilder>, var status: GroupStatus) {
    internal val group: GroupOfMethodMocks by lazy { GroupOfMethodMocks(name, methodMocks.map { it.executableMethodMock }, status) }
}
