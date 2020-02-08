package ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl

import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.StubsGroup

data class GroupOfMethodStubs (val name: String, val methodStubs: List<MethodStub>, var status: GroupStatus) : StubsGroup {
    override fun stubsCount(): Int = methodStubs.size

    override fun name(): String = name

    override fun activate() {
        methodStubs.forEach{ it.activate() }
        status = GroupStatus.ACTIVE
    }

    override fun deactivate() {
        methodStubs.forEach{ it.deactivate() }
        status = GroupStatus.NON_ACTIVE
    }

    override fun status(): GroupStatus = status
}

