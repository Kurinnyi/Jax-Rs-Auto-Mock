package ua.kurinnyi.jaxrs.auto.mock.apiv2

import ua.kurinnyi.jaxrs.auto.mock.Utils
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.CompleteMocksData
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.ProxyConfig
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ApiAdapterForResponseGeneration
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.ExecutableMethodMock
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl.GroupOfMethodMocks
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class ContextState<Resource>(clazz: Class<Resource>) {
    internal var priority = 0
    internal val groupCallbacks: MutableList<GroupCallback> = mutableListOf()
    internal var proxyConfig: ProxyConfig = ProxyConfig(emptyMap(), emptyList())

    private var tempArgList: MutableList<ExecutableMethodMock.ArgumentMatcher> = mutableListOf()
    private var captors: MutableList<Captor<Any?>?> = mutableListOf()

    private var methodMocksUnderConstruction: MutableList<MethodMockBuilder> = mutableListOf()
    private var groupsUnderConstruction: MutableList<GroupBuilder> = mutableListOf()

    private val constructedMethodMocks: MutableList<MethodMockBuilder> = mutableListOf()
    private val constructedGroups: MutableList<GroupBuilder> = mutableListOf()

    internal val instance = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { _, method, args ->
        checkAllArgumentsSetUp(args, method)
        val methodMockBuilder = MethodMockBuilder(method, tempArgList, captors)
        methodMocksUnderConstruction.add(methodMockBuilder)
        tempArgList = mutableListOf()
        captors = mutableListOf()
        groupsUnderConstruction.forEach { group -> group.methodMocks.add(methodMockBuilder) }
        methodMockBuilder.isActivatedByGroups = groupsUnderConstruction
                .lastOrNull()?.let { it.status == GroupStatus.ACTIVE } ?: true
        Utils.getReturnValue(method)
    } as Resource

    internal fun addMatcher(matchType: ExecutableMethodMock.MatchType, matcher: (Any?) -> Boolean) {
        tempArgList.add(ExecutableMethodMock.ArgumentMatcher(matchType, matcher))
    }

    internal fun <T> addResponseSection(response: ResponseContext<T>.() -> T) {
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

    internal fun <T> addGroup(activeByDefault: Boolean, name: String, body: () -> T): T {
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

    internal fun addRequestHeader(value: String?, name: String) {
        if (tempArgList.isEmpty()) {
            tempArgList.add(ExecutableMethodMock.ArgumentMatcher(ExecutableMethodMock.MatchType.MATCH) {
                actualValue -> actualValue == value
            })
        }
        methodMocksUnderConstruction.forEach {
            it.requestHeaders += ExecutableMethodMock.HeaderParameter(name, tempArgList.last())
        }
        tempArgList = mutableListOf()
    }

    internal fun addCaptor(captor: Captor<Any?>) {
        if (tempArgList.isEmpty())
            throw IllegalStateException("Captor is used incorrectly. It should wrap argument matcher like 'captorName(any())'.")
        while (captors.size < tempArgList.size - 1) {
            captors.add(null)
        }
        captors.add(captor)
    }

    internal fun getCompleteMockData(): CompleteMocksData {
        if (methodMocksUnderConstruction.isNotEmpty())
            throw IllegalStateException("There are some method mocks without response section")
        return CompleteMocksData(
                constructedMethodMocks.map { it.executableMethodMock },
                constructedGroups.map { it.group },
                proxyConfig)
    }

    private fun checkAllArgumentsSetUp(args: Array<Any>?, method: Method) {
        if (args != null && tempArgList.size < args.size) {
            throw IllegalArgumentException("Not all parameters of method: ${method.name} are " +
                    "called with matchers. Use match methods like 'eq','any' etc for all the parameters")
        }
    }

    private fun checkReturnTypesOfMockedMethods() =
            methodMocksUnderConstruction.asSequence().map { it.method }
                    .windowed(2)
                    .filter { (m1, m2) -> m1.returnType != m2.returnType}
                    .forEach { (m1, m2) ->
                        throw IllegalStateException("Trying to have a single response " +
                                "section for methods with different return types: $m1, $m2")
                    }

    private data class MethodMockBuilder(
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

    private data class GroupBuilder(val name: String, val methodMocks: MutableList<MethodMockBuilder>, var status: GroupStatus) {
        internal val group: GroupOfMethodMocks by lazy { GroupOfMethodMocks(name, methodMocks.map { it.executableMethodMock }, status) }
    }
}

