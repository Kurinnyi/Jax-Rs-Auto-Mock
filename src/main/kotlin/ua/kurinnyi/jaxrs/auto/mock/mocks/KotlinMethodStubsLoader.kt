package ua.kurinnyi.jaxrs.auto.mock.mocks

import ua.kurinnyi.jaxrs.auto.mock.mocks.model.StubsGroup
import ua.kurinnyi.jaxrs.auto.mock.MethodStubsLoader
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.Group
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class KotlinMethodStubsLoader(stubDefinitions: List<StubsDefinition>, proxyConfiguration: ProxyConfiguration) : MethodStubsLoader {
    private val staticLoadedStubs: LoadedStubs
    private var allLoadedStubs: LoadedStubs

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

    private fun mergeGroups(allGroups: List<Group>): List<Group> {
        val groupedByNameGroups = allGroups.groupBy { it.name }
        return groupedByNameGroups.values.map { sameNameGroups -> sameNameGroups.reduce(Group::merge) }
    }

    override fun getStubs(): List<ResourceMethodStub> = allLoadedStubs.stubs
    override fun getGroups(): List<StubsGroup> = allLoadedStubs.groups
    override fun getGroupsCallbacks(): List<GroupCallback> = allLoadedStubs.groupsCallbacks

    data class LoadedStubs(val stubs: List<ResourceMethodStub>, val groups: List<Group>, val groupsCallbacks: List<GroupCallback>)
}