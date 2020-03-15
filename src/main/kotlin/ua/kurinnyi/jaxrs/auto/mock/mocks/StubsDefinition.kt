package ua.kurinnyi.jaxrs.auto.mock.mocks

import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.CompleteMocksData

interface StubsDefinition {
    fun getStubs(): CompleteMocksData = CompleteMocksData()
    fun getPriority(): Int = 0
    fun getGroupsCallbacks(): List<GroupCallback> = emptyList()
    fun isHotReloadable(): Boolean = false
}

