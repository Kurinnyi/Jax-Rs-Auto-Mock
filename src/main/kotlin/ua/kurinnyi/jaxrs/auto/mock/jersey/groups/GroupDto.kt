package ua.kurinnyi.jaxrs.auto.mock.jersey.groups

import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus

class GroupDto(var name:String, var status:GroupStatus, var mocks:Int) {
    constructor() : this("", GroupStatus.ACTIVE, 0)
}