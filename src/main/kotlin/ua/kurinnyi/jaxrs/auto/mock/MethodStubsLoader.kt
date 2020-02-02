package ua.kurinnyi.jaxrs.auto.mock

import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupCallback
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.ResourceMethodStub
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.StubsGroup

interface MethodStubsLoader {
    fun getStubs(): List<ResourceMethodStub>
    fun getGroups(): List<StubsGroup>
    fun getGroupsCallbacks(): List<GroupCallback>
}