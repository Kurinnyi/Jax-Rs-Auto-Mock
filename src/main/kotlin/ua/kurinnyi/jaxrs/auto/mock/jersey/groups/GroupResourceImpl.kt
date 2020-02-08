package ua.kurinnyi.jaxrs.auto.mock.jersey.groups

import ua.kurinnyi.jaxrs.auto.mock.jersey.JerseyDependenciesRegistry
import ua.kurinnyi.jaxrs.auto.mock.GroupSwitchService
import ua.kurinnyi.jaxrs.auto.mock.apiv1.StubDefinitionContext
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition

class GroupResourceImpl : StubsDefinition {

    private val groupSwitchService:GroupSwitchService by lazy { JerseyDependenciesRegistry.groupSwitchService() }

    override fun getStubs() = StubDefinitionContext().createStubs {
        forClass(GroupResource::class) {
            case { getAll() } then {
                groupSwitchService.getAllGroups().map { group ->
                    GroupDto(group.name(), group.status(), group.stubsCount())
                }.sortedBy { group -> group.status }
            }

            case { updateList(any()) } then1 { groups: List<GroupDto> ->
                groups.forEach { group -> groupSwitchService.switchGroupStatus(group.name, group.status) }
            }
        }
    }
}