package ua.kurinnyi.jaxrs.auto.mock

import ua.kurinnyi.jaxrs.auto.mock.kotlin.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.model.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.model.StubsGroup

interface MethodStubsLoader {
    fun getStubs(): List<ResourceMethodStub>
    fun getGroups(): List<StubsGroup>
    fun getGroupsCallbacks(): List<GroupCallback>
}