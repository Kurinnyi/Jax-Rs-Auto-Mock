package ua.kurinnyi.jaxrs.auto.mock.mocks.model

interface StubsGroup {
    fun name():String
    fun activate()
    fun deactivate()
    fun status(): GroupStatus
    fun stubsCount(): Int
}
enum class GroupStatus {
    ACTIVE, PARTIALLY_ACTIVE, NON_ACTIVE
}