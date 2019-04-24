package ua.kurinnyi.jaxrs.auto.mock.kotlin

interface StubsDefinition {
    fun getStubs(context: StubDefinitionContext): Pair<List<MethodStub>, List<Group>> = emptyList<MethodStub>() to emptyList()
    fun getPriority(): Int = 0
    fun getGroupsCallbacks(context: GroupsCallbacksContext): List<GroupCallback> = emptyList()
}

