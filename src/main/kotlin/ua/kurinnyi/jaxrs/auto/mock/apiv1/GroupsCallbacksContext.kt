package ua.kurinnyi.jaxrs.auto.mock.apiv1

import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback

class GroupsCallbacksContext {

    private val  callbacks:MutableList<GroupCallback> = mutableListOf()

    fun addGroupCallbacks(definitions: GroupsCallbacksContext.() -> Unit): List<GroupCallback> {
        definitions(this)
        return callbacks
    }

    fun onGroupEnabled(groupName:String, callback: () -> Unit){
        callbacks.add(GroupCallback(groupName = groupName, onGroupEnabled = callback))
    }

    fun onGroupDisabled(groupName:String, callback: () -> Unit){
        callbacks.add(GroupCallback(groupName = groupName, onGroupDisabled = callback))
    }
}
