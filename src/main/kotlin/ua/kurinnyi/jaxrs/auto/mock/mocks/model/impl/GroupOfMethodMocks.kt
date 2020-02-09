package ua.kurinnyi.jaxrs.auto.mock.mocks.model.impl

import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus
import ua.kurinnyi.jaxrs.auto.mock.mocks.model.Group

data class GroupOfMethodMocks (val name: String, val mocks: List<ExecutableMethodMock>, var status: GroupStatus) : Group {
    override fun mocksCount(): Int = mocks.size

    override fun name(): String = name

    override fun activate() {
        mocks.forEach{ it.activate() }
        status = GroupStatus.ACTIVE
    }

    override fun deactivate() {
        mocks.forEach{ it.deactivate() }
        status = GroupStatus.NON_ACTIVE
    }

    override fun status(): GroupStatus = status
}

