package ua.kurinnyi.jaxrs.auto.mock.endpoint

import ua.kurinnyi.jaxrs.auto.mock.MethodStubsLoader
import ua.kurinnyi.jaxrs.auto.mock.kotlin.StubDefinitionContext
import ua.kurinnyi.jaxrs.auto.mock.kotlin.StubsDefinition
import ua.kurinnyi.jaxrs.auto.mock.model.GroupStatus

class GroupResourceImpl : StubsDefinition {
    lateinit var loader: MethodStubsLoader

    override fun getStubs(context: StubDefinitionContext) = context.createStubs {
        forClass(GroupResource::class) {
            whenRequest { getAll() } thenResponse {
                bodyProvider {
                    loader.getGroups().map { group ->
                        GroupDto(group.name(), group.status(), group.stubsCount())
                    }.sortedBy { group -> group.status }
                }
            }

            whenRequest { updateList(any()) } thenResponse {
                bodyProvider { (groups) ->
                    val groupDto = groups as List<GroupDto>
                    groupDto.forEach { group ->
                        val stubsGroup = loader.getGroups().find { it.name() == group.name }
                        stubsGroup ?: println("Group with name ${group.name} not found")
                        when (group.status) {
                            GroupStatus.ACTIVE -> stubsGroup?.activate()
                            GroupStatus.NON_ACTIVE -> stubsGroup?.deactivate()
                            GroupStatus.PARTIALLY_ACTIVE ->
                                println("PARTIALLY_ACTIVE is not a status that can be set by endpoint")
                        }
                    }
                }
            }

        }
    }
}