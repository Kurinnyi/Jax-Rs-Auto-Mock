package ua.kurinnyi.jaxrs.auto.mock.mocks

import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.StubDefinitionData

interface StubsDefinition {
    fun getStubs(): StubDefinitionData = StubDefinitionData()
    fun getPriority(): Int = 0
    fun getGroupsCallbacks(): List<GroupCallback> = emptyList()
    fun isRealTime(): Boolean = false
}

