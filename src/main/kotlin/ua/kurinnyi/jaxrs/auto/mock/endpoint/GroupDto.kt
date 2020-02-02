package ua.kurinnyi.jaxrs.auto.mock.endpoint

import ua.kurinnyi.jaxrs.auto.mock.mocks.model.GroupStatus

class GroupDto(var name:String, var status:GroupStatus, var stubs:Int) {
    constructor() : this("", GroupStatus.ACTIVE, 0)
}