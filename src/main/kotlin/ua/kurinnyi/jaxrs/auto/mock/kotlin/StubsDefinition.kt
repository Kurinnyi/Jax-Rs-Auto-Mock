package ua.kurinnyi.jaxrs.auto.mock.kotlin

interface StubsDefinition {
    fun getStubs(context: StubDefinitionContext): Pair<List<MethodStub>, List<Group>> = emptyList<MethodStub>() to emptyList()
    fun getGroupsCallbacks(context: GroupsCallbacksContext): List<GroupCallback> = emptyList()

}

