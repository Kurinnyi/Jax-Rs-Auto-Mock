package ua.kurinnyi.jaxrs.auto.mock.mocks

import ua.kurinnyi.jaxrs.auto.mock.extensions.ProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class StubDefinitionsExecutor(stubDefinitions: List<StubsDefinition>, proxyConfiguration: ProxyConfiguration) {
    private val staticLoadedMockData: LoadedMockData
    private var allLoadedMockData: LoadedMockData

    fun getMocks(): List<MethodMock> = allLoadedMockData.mocks
    fun getGroups(): List<Group> = allLoadedMockData.groups
    fun getGroupsCallbacks(): List<GroupCallback> = allLoadedMockData.groupsCallbacks

    init {
        val (hotReloadableStubs, staticStubs) = stubDefinitions.partition { it.isHotReloadable() }
        staticLoadedMockData = executeStubsDefinitions(staticStubs, proxyConfiguration)
        allLoadedMockData = loadHotMockDataAndMerge(hotReloadableStubs, proxyConfiguration)
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            allLoadedMockData = loadHotMockDataAndMerge(hotReloadableStubs, proxyConfiguration)
        }, 0, 10, TimeUnit.SECONDS)
    }

    private fun loadHotMockDataAndMerge(hotReloadableStubs: List<StubsDefinition>, proxyConfiguration: ProxyConfiguration): LoadedMockData {
        val hotLoadedMocks = executeStubsDefinitions(hotReloadableStubs, proxyConfiguration)
        return LoadedMockData(
                (staticLoadedMockData.mocksWithPriority + hotLoadedMocks.mocksWithPriority),
                mergeGroups(hotLoadedMocks.groups + staticLoadedMockData.groups),
                staticLoadedMockData.groupsCallbacks + hotLoadedMocks.groupsCallbacks)
    }

    private fun executeStubsDefinitions(stubs: List<StubsDefinition>, proxyConfiguration: ProxyConfiguration): LoadedMockData {
        val definitions: List<Pair<CompleteMocksData, Int>> = stubs.asSequence()
                .map { it.getStubs() to it.getPriority() }.toList()
        definitions.map { it.first }.forEach {
            it.proxyConfig.proxyClasses.forEach { (clazz, path) -> proxyConfiguration.addClassForProxy(clazz, path) }
            it.proxyConfig.recordClasses.forEach { clazz -> proxyConfiguration.addClassForRecord(clazz) }
        }
        return LoadedMockData(
                definitions.flatMap { (mockData, priority) -> mockData.methodMocks.map { MockWithPriority(it, priority) } },
                mergeGroups(definitions.flatMap { (mockData, _) -> mockData.groups }),
                stubs.flatMap { it.getGroupsCallbacks() })
    }

    private fun mergeGroups(allGroups: List<Group>): List<Group> {
        return allGroups.groupBy { it.name() }.toList()
                .map { (name, groupsToMerge) ->  GroupsAggregation(name, groupsToMerge)}
    }

    data class LoadedMockData(
            val mocksWithPriority: List<MockWithPriority>,
            val groups: List<Group>,
            val groupsCallbacks: List<GroupCallback>) {
        val mocks:List<MethodMock> by lazy {
            mocksWithPriority.sortedBy { it.priority }.map { it.mock }
        }
    }

    data class MockWithPriority(val mock:MethodMock, val priority: Int)

    data class GroupsAggregation(val name: String, val groups: List<Group>) : Group {
        override fun name(): String = name

        override fun activate() = groups.forEach{ g -> g.activate()}

        override fun deactivate() = groups.forEach{ g -> g.deactivate()}

        override fun status(): GroupStatus = groups.map { it.status() }
                .reduceRight{ s1, s2 -> if (s1 != s2) GroupStatus.PARTIALLY_ACTIVE else s1 }

        override fun mocksCount(): Int = groups.map { it.mocksCount() }.sum()
    }
}