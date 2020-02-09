package ua.kurinnyi.jaxrs.auto.mock.mocks.model

data class CompleteMocksData(
        val methodMocks: List<MethodMock> = emptyList(),
        val groups: List<Group> = emptyList(),
        val proxyConfig: ProxyConfig = ProxyConfig(emptyMap(), emptyList()))

data class ProxyConfig(val proxyClasses:Map<String, String>, val recordClasses:List<String>)

data class GroupCallback(
        val groupName:String,
        val onGroupEnabled: () -> Unit = {},
        val onGroupDisabled: () -> Unit = {})