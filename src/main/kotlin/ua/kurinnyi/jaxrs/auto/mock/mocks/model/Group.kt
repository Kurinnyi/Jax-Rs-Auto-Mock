package ua.kurinnyi.jaxrs.auto.mock.mocks.model

data class Group (val name: String, val methodStubs: List<MethodStub>, var status: GroupStatus) : StubsGroup {
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

    fun merge(other: Group): Group {
        val newStatus = if (status == other.status) status else GroupStatus.PARTIALLY_ACTIVE
        return Group(name, methodStubs + other.methodStubs, newStatus)
    }

    override fun equals(other: Any?): Boolean = (this === other) || (other is Group && other.name == name)

    override fun hashCode(): Int = name.hashCode()
}

