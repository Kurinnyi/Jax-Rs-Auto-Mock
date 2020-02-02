package ua.kurinnyi.jaxrs.auto.mock.mocks.model

data class StubDefinitionData(
        val methodStubs: List<MethodStub> = emptyList(),
        val groups: List<Group> = emptyList(),
        val proxyConfig: CommonProxyConfig = CommonProxyConfig(emptyMap(), emptyList()))

data class CommonProxyConfig(val proxyClasses:Map<String, String>, val recordClasses:List<String>)

data class GroupCallback(
        val groupName:String,
        val onGroupEnabled: () -> Unit = {},
        val onGroupDisabled: () -> Unit = {})