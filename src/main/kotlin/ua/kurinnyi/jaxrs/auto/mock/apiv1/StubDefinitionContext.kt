package ua.kurinnyi.jaxrs.auto.mock.apiv1

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.FileBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.JacksonBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.jersey.JerseyInternalBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ApiAdapterForResponseGeneration
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.*
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.GroupOfMethodStubs
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.MethodStub
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass


class StubDefinitionContext {
    internal val stubs: MutableList<MethodStubBuilder> = mutableListOf()
    private val allGroups: MutableList<GroupBuilder> = mutableListOf()
    internal var activeGroups: List<GroupBuilder> = listOf()
    internal var proxyConfig: CommonProxyConfig = CommonProxyConfig(emptyMap(), emptyList())

    fun createStubs(definitions: StubDefinitionContext.() -> Unit): StubDefinitionData {
        definitions(this)
        return StubDefinitionData(stubs.map { it.methodStub }, allGroups.map { it.group }, proxyConfig)
    }

    fun <RESOURCE : Any> forClass(clazz: KClass<RESOURCE>, definitions: ClazzStubDefinitionContext<RESOURCE>.() -> Unit) {
        definitions(ClazzStubDefinitionContext(clazz.java, this))
    }

    fun group(name: String, activeByDefault:Boolean = true, body:() -> Unit){
        val status = if (activeByDefault) GroupStatus.ACTIVE else GroupStatus.NON_ACTIVE
        val group = GroupBuilder(name, emptyList(), status)
        if (activeGroups.contains(group)) throw IllegalArgumentException("Group $name contains itself recursively. This is forbidden.")
        activeGroups + group
        body()
        allGroups.add(activeGroups.last())
        activeGroups - activeGroups.last()
    }
}

class ClazzStubDefinitionContext<RESOURCE>(private val clazz: Class<RESOURCE>, private val context: StubDefinitionContext) {
    private var tempArgList: List<MethodStub.ArgumentMatcher> = listOf()
    private var methodStubs: List<MethodStubBuilder> = listOf()

    fun bypassAnyNotMatched(path: String) {
        val proxyConfig = context.proxyConfig
        context.proxyConfig = proxyConfig.copy(proxyClasses = proxyConfig.proxyClasses + (clazz.name to path))
    }

    fun recordAnyBypassed() {
        val proxyConfig = context.proxyConfig
        context.proxyConfig = proxyConfig.copy(recordClasses = proxyConfig.recordClasses + clazz.name)
    }

    fun <RESULT> case(methodCall: RESOURCE.() -> RESULT): MethodStubDefinitionRequestContext<RESULT> {
        methodStubs = listOf()
        val instance = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { proxy, method, args ->
            checkAllArgumentsSetUp(args, method)
            methodStubs + MethodStubBuilder(method, tempArgList)
            tempArgList = listOf()
            getReturnValue(method)
        } as RESOURCE
        methodCall(instance)

        val activeByGroup = context.activeGroups.lastOrNull()?.let { it.status == GroupStatus.ACTIVE } ?: true
        context.activeGroups = context.activeGroups
                .map { group -> group.copy(methodStubs = group.methodStubs + methodStubs) }
        context.stubs.addAll(methodStubs)
        methodStubs.forEach{ it.isActivatedByGroups = activeByGroup }
        return MethodStubDefinitionRequestContext(methodStubs)
    }

    private fun checkAllArgumentsSetUp(args: Array<Any>?, method: Method) {

        if (args != null && tempArgList.size < args.size) {
            throw IllegalArgumentException("Not all parameters of method: ${method.name} are " +
                    "called with matchers. Use match methods like 'eq','any' etc for all the parameters")
        }
    }

    fun <ARGUMENT> eq(argument: ARGUMENT): ARGUMENT {
        matchNullable<ARGUMENT?> { it == argument }
        return argument
    }

    fun <ARGUMENT> notEq(argument: ARGUMENT): ARGUMENT {
        matchNullable<ARGUMENT?> { it != argument }
        return argument
    }

    fun <ARGUMENT> any(): ARGUMENT = matchNullable { true }
    fun anyBoolean(): Boolean = matchPrimitive({ true }, true)
    fun anyInt(): Int = matchPrimitive({ true }, 0)
    fun anyDouble(): Double = matchPrimitive({ true }, 0.0)
    fun anyLong(): Long = matchPrimitive({ true }, 0L)

    fun anyBooleanInRecord(): Boolean = ignoreInRecordPrimitive(true)
    fun anyIntInRecord(): Int = ignoreInRecordPrimitive(0)
    fun anyDoubleInRecord(): Double = ignoreInRecordPrimitive(0.0)
    fun anyLongInRecord(): Long = ignoreInRecordPrimitive(0L)

    fun <ARGUMENT> isNull(): ARGUMENT = matchNullable { it == null }

    fun <ARGUMENT> notNull(): ARGUMENT = matchNullable { it != null }

    fun <ARGUMENT> anyInRecord(): ARGUMENT {
        val castedPredicate = { arg: Any? -> true }
        tempArgList + MethodStub.ArgumentMatcher(MethodStub.MatchType.IGNORE_IN_RECORD, castedPredicate)
        return null as ARGUMENT
    }

    fun <ARGUMENT> matchNullable(predicate: (ARGUMENT?) -> Boolean): ARGUMENT {
        val castedPredicate = { arg: Any? -> predicate(arg as ARGUMENT) }
        tempArgList + MethodStub.ArgumentMatcher(MethodStub.MatchType.MATCH, castedPredicate)
        return null as ARGUMENT
    }

    fun <ARGUMENT> match(predicate: (ARGUMENT) -> Boolean): ARGUMENT = matchNullable { it != null && predicate(it) }

    fun matchBoolean(predicate: (Boolean) -> Boolean): Boolean {
        matchNullable<Boolean?> { it != null && predicate(it) }
        return true
    }

    fun matchInt(predicate: (Int) -> Boolean): Int {
        matchNullable<Int?> { it != null && predicate(it) }
        return 0
    }

    fun matchDouble(predicate: (Double) -> Boolean): Double {
        matchNullable<Double?> { it != null && predicate(it) }
        return 0.0
    }

    fun matchLong(predicate: (Long) -> Boolean): Long {
        matchNullable<Long?> { it != null && predicate(it) }
        return 0L
    }

    private fun <ARGUMENT> matchPrimitive(predicate: (ARGUMENT?) -> Boolean, arg: ARGUMENT): ARGUMENT {
        matchNullable(predicate)
        return arg
    }

    private fun <ARGUMENT> ignoreInRecordPrimitive(arg: ARGUMENT): ARGUMENT {
        anyInRecord<ARGUMENT>()
        return arg
    }

    fun <ARGUMENT> bodyMatch(predicate: (String) -> Boolean): ARGUMENT {
        val castedPredicate = { arg: Any? -> predicate(arg as String) }
        tempArgList + MethodStub.ArgumentMatcher(MethodStub.MatchType.BODY_MATCH, castedPredicate)
        return null as ARGUMENT
    }

    fun <ARGUMENT> bodyMatchRegex(regex: String): ARGUMENT =
            bodyMatch { regex.toRegex().matches(it) }

    fun <ARGUMENT> bodySameJson(body: String): ARGUMENT =
            bodyMatch { Utils.trimToSingleSpaces(body) == Utils.trimToSingleSpaces(it) }

}

class MethodStubDefinitionRequestContext<RESULT>(private val methodStubs: List<MethodStubBuilder>) {

    infix fun with(additionalRequestDefinition: MethodStubDefinitionRequestParamsContext.() -> Unit)
            : MethodStubDefinitionRequestContext<RESULT> {
        additionalRequestDefinition(MethodStubDefinitionRequestParamsContext(methodStubs))
        return this
    }

    infix fun then(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(Array<Any?>) -> RESULT?) {
        setResponseDefinitionToMethodStubs{ responseContext, args -> responseContext.responseDefinition(args)}
    }
    infix fun <A1> then1(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(A1) -> RESULT?) {
        setResponseDefinitionToMethodStubs{ responseContext, args -> responseContext.responseDefinition(args[0] as A1)}
    }
    infix fun <A1, A2> then2(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(A1, A2) -> RESULT?) {
        setResponseDefinitionToMethodStubs{ responseContext, args -> responseContext.responseDefinition(args[0] as A1, args[1] as A2)}
    }
    infix fun <A1, A2, A3> then3(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(A1, A2, A3) -> RESULT?) {
        setResponseDefinitionToMethodStubs{ responseContext, args ->
            responseContext.responseDefinition(args[0] as A1, args[1] as A2, args[2] as A3)}
    }
    infix fun <A1, A2, A3, A4> then4(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(A1, A2, A3, A4) -> RESULT?) {
        setResponseDefinitionToMethodStubs{ responseContext, args ->
            responseContext.responseDefinition(args[0] as A1, args[1] as A2, args[2] as A3, args[3] as A4)}
    }
    infix fun <A1, A2, A3, A4, A5> then5(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(A1, A2, A3, A4, A5) -> RESULT?) {
        setResponseDefinitionToMethodStubs{ responseContext, args ->
            responseContext.responseDefinition(args[0] as A1, args[1] as A2, args[2] as A3, args[3] as A4, args[4] as A5)}
    }
    private fun setResponseDefinitionToMethodStubs(application: (MethodStubDefinitionResponseContext<RESULT?>, Array<Any?>) -> RESULT?) {
        methodStubs.forEach {
            it.responseSection = { apiAdapter, args, methodStub ->
                application(MethodStubDefinitionResponseContext(apiAdapter, methodStub), args)
            }
        }
    }
}

class MethodStubDefinitionRequestParamsContext(private val methodStubs: List<MethodStubBuilder>) {

    fun header(name: String, value: HeaderValue) {
        methodStubs.forEach { it.requestHeaders += MethodStub.HeaderParameter(name, value.matcher) }
    }

    class HeaderValue private constructor(val matcher: MethodStub.ArgumentMatcher) {
        companion object {
            fun forMatcher(matcher: MethodStub.ArgumentMatcher): HeaderValue {
                return HeaderValue(matcher)
            }
        }
    }

    fun eq(value: String): HeaderValue = matchNullable { it == value }

    fun match(predicate: (String) -> Boolean): HeaderValue = matchNullable { it != null && predicate(it) }

    fun matchNullable(predicate: (String?) -> Boolean): HeaderValue {
        val castedPredicate = { arg: Any? -> predicate(arg as String?) }
        return HeaderValue.forMatcher(MethodStub.ArgumentMatcher(MethodStub.MatchType.MATCH, castedPredicate))
    }

    fun notEq(value: String): HeaderValue = matchNullable { it != value }

    fun isNull(): HeaderValue = matchNullable { it == null }

    fun notNull(): HeaderValue = matchNullable { it != null }

    fun matchRegex(regex: String): HeaderValue = matchNullable { it != null && regex.toRegex().matches(it) }
}

class MethodStubDefinitionResponseContext<RESPONSE> (
        private val apiAdapter: ApiAdapterForResponseGeneration,
        private val methodStub: MethodStub) {

    fun record() {
        apiAdapter.recordResponse(methodStub.arguments)
    }

    fun code(code: Int): RESPONSE? {
        apiAdapter.setResponseCode(code)
        return getReturnValue(apiAdapter.method)
    }

    fun bodyRaw(body:String): RESPONSE? {
        apiAdapter.writeBodyRaw(body)
        return getReturnValue(apiAdapter.method)
    }

    fun proxyTo(path: String): RESPONSE? {
        apiAdapter.proxyTo(path)
        return getReturnValue(apiAdapter.method)
    }

    fun header(headerName: String, headerValue: String): RESPONSE? {
        apiAdapter.header(headerName, headerValue)
        return getReturnValue(apiAdapter.method)
    }

    fun bodyJson(bodyProvider: BodyProvider, body: String, vararg templateArgs: Pair<String, Any>): RESPONSE =
            apiAdapter.getObjectFromString(bodyProvider, body, templateArgs.toMap())

    fun bodyJson(body: String, vararg templateArgs: Pair<String, Any>): RESPONSE =
            apiAdapter.getObjectFromString(body, templateArgs.toMap())
}

data class MethodStubBuilder (private val method: Method, val arguments: List<MethodStub.ArgumentMatcher>) {
    internal var requestHeaders: List<MethodStub.HeaderParameter> = listOf()
    internal var responseSection:  ((ApiAdapterForResponseGeneration, Array<Any?>, MethodStub) -> Any?)? = null
    internal var isActivatedByGroups: Boolean = true

    internal val methodStub: MethodStub by lazy {
        responseSection?.let { MethodStub(method, arguments, requestHeaders, it, isActivatedByGroups) }
                ?: throw IllegalStateException("Haven't you forgot to add 'then' section to mock for ${method.name}")
    }
}

data class GroupBuilder (val name: String, val methodStubs: List<MethodStubBuilder>, var status: GroupStatus) {
    internal val group: GroupOfMethodStubs by lazy { GroupOfMethodStubs(name, methodStubs.map { it.methodStub }, status) }

    override fun equals(other: Any?): Boolean = (this === other) || (other is GroupBuilder && other.name == name)

    override fun hashCode(): Int = name.hashCode()
}

private fun <T>getReturnValue(method: Method): T? {
    val result =  when (method.returnType) {
        Int::class.java -> 0
        Long::class.java -> 0L
        Double::class.java -> 0.0
        Float::class.java -> false
        Boolean::class.java -> false
        else -> null
    }
    return result as T?
}
typealias BY_JACKSON = JacksonBodyProvider
typealias BY_JERSEY = JerseyInternalBodyProvider
typealias FROM_FILE = FileBodyProvider