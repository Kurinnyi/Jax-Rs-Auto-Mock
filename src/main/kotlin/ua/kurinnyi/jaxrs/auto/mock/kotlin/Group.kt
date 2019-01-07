package ua.kurinnyi.jaxrs.auto.mock.kotlin

import ua.kurinnyi.jaxrs.auto.mock.model.StubsGroup
import ua.kurinnyi.jaxrs.auto.mock.model.GroupStatus

data class Group (val name: String, val methodStubs: List<MethodStub>, var status: GroupStatus) : StubsGroup {
    override fun stubsCount(): Int = methodStubs.size

    override fun name(): String = name

    override fun activate() {
        methodStubs.forEach{ it.isActivatedByGroups = true}
        status = GroupStatus.ACTIVE
    }

    override fun deactivate() {
        methodStubs.forEach{ it.isActivatedByGroups = false}
        status = GroupStatus.NON_ACTIVE
    }

    override fun status(): GroupStatus = status

    fun merge(other:Group):Group{
        val newStatus = if (status == other.status) status else GroupStatus.PARTIALLY_ACTIVE
        return Group(name, methodStubs + other.methodStubs, newStatus)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Group && other.name == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

