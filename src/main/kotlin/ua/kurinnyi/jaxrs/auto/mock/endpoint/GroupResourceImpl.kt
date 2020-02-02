package ua.kurinnyi.jaxrs.auto.mock.endpoint

import ua.kurinnyi.jaxrs.auto.mock.GroupSwitchService
import ua.kurinnyi.jaxrs.auto.mock.mocks.apiv1.StubDefinitionContext
import ua.kurinnyi.jaxrs.auto.mock.mocks.StubsDefinition

class GroupResourceImpl : StubsDefinition {

    override fun getStubs() = StubDefinitionContext().createStubs {
        forClass(GroupResource::class) {
            case { getAll() } then {
                GroupSwitchService.getAllGroups().map { group ->
                    GroupDto(group.name(), group.status(), group.stubsCount())
                }.sortedBy { group -> group.status }
            }

            case { updateList(any()) } then1 { groups: List<GroupDto> ->
                groups.forEach { group -> GroupSwitchService.switchGroupStatus(group.name, group.status) }
            }
        }
    }
}