package ua.kurinnyi.jaxrs.auto.mock.mocks.model

interface Group {
    fun name():String
    fun activate()
    fun deactivate()
    fun status(): GroupStatus
    fun mocksCount(): Int
}
enum class GroupStatus {
    ACTIVE, PARTIALLY_ACTIVE, NON_ACTIVE
}