package ua.kurinnyi.jaxrs.auto.mock.mocks

import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.StubsGroup
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MethodStubsLoader(stubDefinitions: List<StubsDefinition>, proxyConfiguration: ProxyConfiguration) {
    private val staticLoadedStubs: LoadedStubs
    private var allLoadedStubs: LoadedStubs

    fun getStubs(): List<ResourceMethodStub> = allLoadedStubs.stubs
    fun getGroups(): List<StubsGroup> = allLoadedStubs.groups
    fun getGroupsCallbacks(): List<GroupCallback> = allLoadedStubs.groupsCallbacks

    init {
        val (realTimeStubs, staticStubs) = stubDefinitions.partition { it.isRealTime() }
        staticLoadedStubs = initStubs(staticStubs, proxyConfiguration)
        allLoadedStubs = loadRealTimeStubsAndMerge(realTimeStubs, proxyConfiguration)
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            allLoadedStubs = loadRealTimeStubsAndMerge(realTimeStubs, proxyConfiguration)
        }, 0, 10, TimeUnit.SECONDS)
    }

    private fun loadRealTimeStubsAndMerge(realTimeStubs: List<StubsDefinition>, proxyConfiguration: ProxyConfiguration): LoadedStubs {
        val realTimeLoadedStubs = initStubs(realTimeStubs, proxyConfiguration)
        return LoadedStubs(
                staticLoadedStubs.stubs + realTimeLoadedStubs.stubs,
                mergeGroups(realTimeLoadedStubs.groups + staticLoadedStubs.groups),
                staticLoadedStubs.groupsCallbacks + realTimeLoadedStubs.groupsCallbacks)
    }

    private fun initStubs(stubs: List<StubsDefinition>, proxyConfiguration: ProxyConfiguration): LoadedStubs {
        val definitions = stubs.asSequence().map { it to it.getStubs() }
                .sortedBy { (definition, _) -> definition.getPriority() }
                .map { (_, stubs) -> stubs }.toList()
        definitions.forEach {
            it.proxyConfig.proxyClasses.forEach { (clazz, path) -> proxyConfiguration.addClassForProxy(clazz, path) }
            it.proxyConfig.recordClasses.forEach { clazz -> proxyConfiguration.addClassForRecord(clazz) }
        }
        return LoadedStubs(
                definitions.flatMap { (stubs, _) -> stubs },
                mergeGroups(definitions.flatMap { (_, groups) -> groups }),
                stubs.flatMap { it.getGroupsCallbacks() })
    }

    private fun mergeGroups(allGroups: List<StubsGroup>): List<StubsGroup> {
        return allGroups.groupBy { it.name() }.toList()
                .map { (name, groupsToMerge) ->  GroupsAggregation(name, groupsToMerge)}
    }

    data class LoadedStubs(val stubs: List<ResourceMethodStub>, val groups: List<StubsGroup>, val groupsCallbacks: List<GroupCallback>)

    data class GroupsAggregation(val name: String, val groups: List<StubsGroup>) : StubsGroup {
        override fun name(): String = name

        override fun activate() = groups.forEach{ g -> g.activate()}

        override fun deactivate() = groups.forEach{ g -> g.deactivate()}

        override fun status(): GroupStatus = groups.map { it.status() }
                .reduceRight{ s1, s2 -> if (s1 != s2) GroupStatus.PARTIALLY_ACTIVE else s1 }

        override fun stubsCount(): Int = groups.map { it.stubsCount() }.sum()
    }
}