package ua.kurinnyi.jaxrs.auto.mock

import ua.kurinnyi.jaxrs.auto.mock.mocks.StubDefinitionsExecutor
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.Group

class GroupSwitchService(private val loader: StubDefinitionsExecutor) {

    fun switchGroupStatus(groupName:String, status: GroupStatus){
        val mockGroup = loader.getGroups().find { it.name() == groupName }
        val groupCallbacks = loader.getGroupsCallbacks().filter { it.groupName == groupName }
        mockGroup ?: println("Group with name ${groupName} not found")
        when (status) {
            GroupStatus.ACTIVE -> {
                mockGroup?.activate()
                groupCallbacks.forEach{it.onGroupEnabled()}
            }
            GroupStatus.NON_ACTIVE -> {
                mockGroup?.deactivate()
                groupCallbacks.forEach{it.onGroupDisabled()}
            }
            GroupStatus.PARTIALLY_ACTIVE ->
                println("PARTIALLY_ACTIVE is not a status that can be set by endpoint")
        }
    }

    fun getAllGroups(): List<Group> {
        return loader.getGroups()
    }
}