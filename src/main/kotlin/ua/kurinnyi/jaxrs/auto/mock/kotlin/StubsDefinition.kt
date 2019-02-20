package ua.kurinnyi.jaxrs.auto.mock.kotlin

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.FileBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.JacksonBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.JerseyInternalBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.model.GroupStatus
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

interface StubsDefinition {
    fun getStubs(context: StubDefinitionContext): Pair<List<MethodStub>, List<Group>>
}

class StubDefinitionContext(val proxyConfiguration: ProxyConfiguration) {
    internal val stubs: MutableList<MethodStub> = mutableListOf()
    internal val allGroups: MutableList<Group> = mutableListOf()
    internal var activeGroups: List<Group> = listOf()
    internal var proxyBypassPath: String? = null
    internal var shouldBypassWhenNothingMatched: Boolean = false


    fun bypassAnyNotMatched(path: String) {
        shouldBypassWhenNothingMatched = true
        proxyBypassPath = path
    }

    fun bypassAnyNotMatched() {
        shouldBypassWhenNothingMatched = true
    }


    fun createStubs(definitions: StubDefinitionContext.() -> Unit): Pair<List<MethodStub>, List<Group>> {
        definitions(this)
        return Pair(stubs.toList(), allGroups.toList())
    }

    fun <RESOURCE : Any> forClass(clazz: KClass<RESOURCE>, definitions: ClazzStubDefinitionContext<RESOURCE>.() -> Unit) {
        if (shouldBypassWhenNothingMatched) {
            proxyConfiguration.addClass(clazz.java.name, proxyBypassPath)
        }
        definitions(ClazzStubDefinitionContext(clazz.java, this))
    }

    fun group(name: String, activeByDefault:Boolean = true, body:() -> Unit){
        val status = if (activeByDefault) GroupStatus.ACTIVE else GroupStatus.NON_ACTIVE
        val group = Group(name, emptyList(), status)
        if (activeGroups.contains(group)) throw IllegalArgumentException("Group $name contains itself recursively. This is forbidden.")
        activeGroups += group
        body()
        allGroups.add(activeGroups.last())
        activeGroups -= activeGroups.last()
    }
}

class ClazzStubDefinitionContext<RESOURCE>(private val clazz: Class<RESOURCE>, private val context: StubDefinitionContext) {
    private var tempArgList: List<MethodStub.ArgumentMatcher> = listOf()
    private var methodStubs: List<MethodStub> = listOf()

    fun bypassAnyNotMatched(path: String) {
        context.proxyConfiguration.addClass(clazz.name, path)
    }

    fun bypassAnyNotMatched() {
        context.proxyConfiguration.addClass(clazz.name, null)
    }

    fun <RESULT> case(methodCall: RESOURCE.() -> RESULT): MethodStubDefinitionRequestContext<RESULT> {
        methodStubs = listOf()
        val instance = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { proxy, method, args ->
            checkAllArgumentsSetUp(args, method)
            methodStubs += MethodStub(clazz, method, tempArgList)
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

    fun <ARGUMENT> isNull(): ARGUMENT = matchNullable { it == null }

    fun <ARGUMENT> notNull(): ARGUMENT = matchNullable { it != null }

    fun <ARGUMENT> matchNullable(predicate: (ARGUMENT?) -> Boolean): ARGUMENT {
        val castedPredicate = { arg: Any? -> predicate(arg as ARGUMENT) }
        tempArgList += MethodStub.ArgumentMatcher(MethodStub.MatchType.MATCH, castedPredicate)
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

    fun <ARGUMENT> bodyMatch(predicate: (String) -> Boolean): ARGUMENT {
        val castedPredicate = { arg: Any? -> predicate(arg as String) }
        tempArgList += MethodStub.ArgumentMatcher(MethodStub.MatchType.BODY_MATCH, castedPredicate)
        return null as ARGUMENT
    }

    fun <ARGUMENT> bodyMatchRegex(regex: String): ARGUMENT =
            bodyMatch { regex.toRegex().matches(it) }

    fun <ARGUMENT> bodyJson(body: String): ARGUMENT =
            bodyMatch { Utils.trimToSingleSpaces(body) == Utils.trimToSingleSpaces(it) }

}

class MethodStubDefinitionRequestContext<RESULT>(private val methodStubs: List<MethodStub>) {

    infix fun with(additionalRequestDefinition: MethodStubDefinitionRequestParamsContext.() -> Unit)
            : MethodStubDefinitionRequestContext<RESULT> {
        additionalRequestDefinition(MethodStubDefinitionRequestParamsContext(methodStubs))
        return this
    }

    infix fun then(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(Array<Any?>) -> RESULT?) {
        methodStubs.forEach{ it.responseSection = responseDefinition as MethodStubDefinitionResponseContext<*>.(Array<Any?>) -> RESULT?}
    }

    infix fun <A1> then1(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(A1) -> RESULT?) {
        methodStubs.forEach{ it.responseSection1 = responseDefinition as MethodStubDefinitionResponseContext<*>.(Any?) -> RESULT?}
    }
    infix fun <A1, A2> then2(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(A1, A2) -> RESULT?) {
        methodStubs.forEach{ it.responseSection2 = responseDefinition as MethodStubDefinitionResponseContext<*>.(Any?, Any?) -> RESULT?}
    }
    infix fun <A1, A2, A3> then3(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(A1, A2, A3) -> RESULT?) {
        methodStubs.forEach{ it.responseSection3 = responseDefinition as MethodStubDefinitionResponseContext<*>.(Any?, Any?, Any?) -> RESULT?}
    }
    infix fun <A1, A2, A3, A4> then4(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(A1, A2, A3, A4) -> RESULT?) {
        methodStubs.forEach{ it.responseSection4 = responseDefinition as MethodStubDefinitionResponseContext<*>.(Any?, Any?, Any?, Any?) -> RESULT?}
    }
    infix fun <A1, A2, A3, A4, A5> then5(responseDefinition: MethodStubDefinitionResponseContext<RESULT?>.(A1, A2, A3, A4, A5) -> RESULT?) {
        methodStubs.forEach{ it.responseSection5 = responseDefinition as MethodStubDefinitionResponseContext<*>.(Any?, Any?, Any?, Any?, Any?) -> RESULT?}
    }

}

class MethodStubDefinitionRequestParamsContext(private val methodStubs: List<MethodStub>) {

    fun header(name: String, value: HeaderValue) {
        methodStubs.forEach { it.requestHeaders.add(MethodStub.HeaderParameter(name, value.matcher)) }
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

class MethodStubDefinitionResponseContext<RESPONSE> (private val apiAdapter:ApiAdapter) {

    fun code(code: Int): RESPONSE? {
        apiAdapter.code(code)
        return getReturnValue(apiAdapter.method)
    }

    fun bodyRaw(body:String): RESPONSE? {
        apiAdapter.bodyRaw(body)
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

    fun bodyJson(bodyProvider: BodyProvider, body: String): RESPONSE =
            apiAdapter.getObjectFromJson(bodyProvider, body)

    fun bodyJsonTemplate(bodyProvider: BodyProvider, templateArgs: Map<String, Any>, body: String): RESPONSE =
            apiAdapter.getObjectFromJsonTemplate(bodyProvider, body, templateArgs)

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
