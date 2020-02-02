package ua.kurinnyi.jaxrs.auto.mock

import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.StubsGroup

object GroupSwitchService {

    internal lateinit var loader:MethodStubsLoader

    fun switchGroupStatus(groupName:String, status: GroupStatus){
        val stubsGroup = loader.getGroups().find { it.name() == groupName }
        val groupCallbacks = loader.getGroupsCallbacks().filter { it.groupName == groupName }
        stubsGroup ?: println("Group with name ${groupName} not found")
        when (status) {
            GroupStatus.ACTIVE -> {
                stubsGroup?.activate()
                groupCallbacks.forEach{it.onGroupEnabled()}
            }
            GroupStatus.NON_ACTIVE -> {
                stubsGroup?.deactivate()
                groupCallbacks.forEach{it.onGroupDisabled()}
            }
            GroupStatus.PARTIALLY_ACTIVE ->
                println("PARTIALLY_ACTIVE is not a status that can be set by endpoint")
        }
    }

    fun getAllGroups(): List<StubsGroup> {
        return loader.getGroups()
    }
}