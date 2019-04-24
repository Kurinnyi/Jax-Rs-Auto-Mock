package ua.kurinnyi.jaxrs.auto.mock.endpoint

import ua.kurinnyi.jaxrs.auto.mock.GroupSwitchService
import ua.kurinnyi.jaxrs.auto.mock.kotlin.StubDefinitionContext
import ua.kurinnyi.jaxrs.auto.mock.kotlin.StubsDefinition

class GroupResourceImpl : StubsDefinition {

    override fun getStubs(context: StubDefinitionContext) = context.createStubs {
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