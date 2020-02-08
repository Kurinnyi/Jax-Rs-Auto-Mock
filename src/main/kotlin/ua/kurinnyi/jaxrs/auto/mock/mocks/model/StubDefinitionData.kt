package ua.kurinnyi.jaxrs.auto.mock.mocks.model

data class StubDefinitionData(
        val methodStubs: List<ResourceMethodStub> = emptyList(),
        val groups: List<StubsGroup> = emptyList(),
        val proxyConfig: CommonProxyConfig = CommonProxyConfig(emptyMap(), emptyList()))

data class CommonProxyConfig(val proxyClasses:Map<String, String>, val recordClasses:List<String>)

data class GroupCallback(
        val groupName:String,
        val onGroupEnabled: () -> Unit = {},
        val onGroupDisabled: () -> Unit = {})