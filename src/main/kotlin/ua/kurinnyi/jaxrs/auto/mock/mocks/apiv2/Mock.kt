package ua.kurinnyi.jaxrs.auto.mock.mocks.apiv2

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.mocks.ApiAdapterForResponseGeneration
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.CommonProxyConfig
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.StubDefinitionData
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy


abstract class Mock<Resource : Any> (val definition:Context<Resource>.(Resource) -> Unit): StubsDefinition {

    var context:Context<Resource>? = null

    override fun getStubs(): StubDefinitionData {
        val clazz = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<Resource>
        val instance = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { proxy, method, args ->

        } as Resource
        val context = Context(clazz)
        this.context = context
        definition(context, instance)
        return StubDefinitionData(emptyList(), emptyList(), context.proxyConfig)
    }

    override fun getPriority(): Int {
        return context?.priority ?: throw IllegalStateException("Priority is requested before get stubs method")
    }

    override fun getGroupsCallbacks(): List<GroupCallback> {
        return context?.groupCallbacks ?: throw IllegalStateException("Group callback is requested before get stubs method")
    }
}
class Context<Resource>(val clazz: Class<Resource>) {

    internal var priority = 0
    internal val groupCallbacks:MutableList<GroupCallback> = mutableListOf()
    internal var proxyConfig:CommonProxyConfig = CommonProxyConfig(emptyMap(), emptyList())

    fun priority(priority:Int) {
        this.priority = priority
    }

    fun onGroupEnabled(groupName:String, callback: () -> Unit){
        groupCallbacks.add(GroupCallback(groupName, onGroupEnabled = callback))
    }

    fun onGroupDisabled(groupName:String, callback: () -> Unit){
        groupCallbacks.add(GroupCallback(groupName, onGroupDisabled = callback))
    }

    fun bypassAnyNotMatched(path: String) {
        proxyConfig = proxyConfig.copy(proxyClasses = mapOf(clazz.name to path))
    }

    fun recordAnyBypassed() {
        proxyConfig = proxyConfig.copy(recordClasses = listOf(clazz.name))
    }

    fun group(name: String, activeByDefault:Boolean = true, body:() -> Unit){

    }

    fun <T> capture():Captor<T> = Captor()

    fun <T> T.respond(response: ResponseContext<T>.() -> T):T {
        return null as T
    }

    operator fun <T> T.invoke(response: ResponseContext<T>.() -> T):T {
        return respond(response)
    }
    fun <T> T.header(name: String, value: String) {

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
        return null as ARGUMENT
    }

    fun <ARGUMENT> matchNullable(predicate: (ARGUMENT?) -> Boolean): ARGUMENT {
        val castedPredicate = { arg: Any? -> predicate(arg as ARGUMENT) }
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
        return null as ARGUMENT
    }

    fun <ARGUMENT> bodyMatchRegex(regex: String): ARGUMENT =
            bodyMatch { regex.toRegex().matches(it) }

    fun <ARGUMENT> bodySameJson(body: String): ARGUMENT =
            bodyMatch { Utils.trimToSingleSpaces(body) == Utils.trimToSingleSpaces(it) }
}
class ResponseContext<Response>(val apiAdapter: ApiAdapterForResponseGeneration) {
    fun record() = apiAdapter.recordResponse()

    fun code(code: Int): Response? {
        apiAdapter.setResponseCode(code)
        return apiAdapter.getReturnValue()
    }

    fun bodyRaw(body:String): Response? {
        apiAdapter.writeBodyRaw(body)
        return apiAdapter.getReturnValue()
    }

    fun proxyTo(path: String): Response? {
        apiAdapter.proxyTo(path)
        return apiAdapter.getReturnValue()
    }

    fun header(headerName: String, headerValue: String): Response? {
        apiAdapter.setResponseHeader(headerName, headerValue)
        return apiAdapter.getReturnValue()
    }

    fun bodyJson(bodyProvider: BodyProvider, body: String, vararg templateArgs: Pair<String, Any>): Response =
            apiAdapter.getObjectFromJson(bodyProvider, body, templateArgs.toMap())

    fun bodyJson(body: String, vararg templateArgs: Pair<String, Any>): Response =
            apiAdapter.getObjectFromJson(body, templateArgs.toMap())
}

class Captor<T> {
    operator fun invoke(value:T):T {
        return value
    }

    operator fun invoke():T {
        return null as T
    }
}