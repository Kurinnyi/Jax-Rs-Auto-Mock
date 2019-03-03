package ua.kurinnyi.jaxrs.auto.mock.kotlin

import ua.kurinnyi.jaxrs.auto.mock.model.StubsGroup
import ua.kurinnyi.jaxrs.auto.mock.MethodStubsLoader
import ua.kurinnyi.jaxrs.auto.mock.model.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.ProxyConfiguration

class KotlinMethodStubsLoader(stubDefinitions: List<StubsDefinition>,  proxyConfiguration: ProxyConfiguration) : MethodStubsLoader {
    private val stubs: List<ResourceMethodStub>
    private val groups: List<StubsGroup>
    private val groupsCallbacks: List<GroupCallback>

    init {
        val definitions = stubDefinitions.map { it.getStubs(StubDefinitionContext(proxyConfiguration)) }
        stubs = definitions.flatMap { (stubs, _) -> stubs }
        groups = mergeGroups(definitions)
        groupsCallbacks = stubDefinitions.flatMap { it.getGroupsCallbacks(GroupsCallbacksContext()) }
    }

    private fun mergeGroups(definitions: List<Pair<List<MethodStub>, List<Group>>>): List<Group> {
        val groupedByNameGroups = definitions.flatMap { (_, groups) -> groups }.groupBy { it.name }
        return groupedByNameGroups.values.map { sameNameGroups -> sameNameGroups.reduce(Group::merge) }
    }

    override fun getStubs(): List<ResourceMethodStub> = stubs
    override fun getGroups(): List<StubsGroup> = groups
    override fun getGroupsCallbacks(): List<GroupCallback> = groupsCallbacks
}