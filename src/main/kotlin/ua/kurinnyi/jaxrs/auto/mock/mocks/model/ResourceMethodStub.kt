package ua.kurinnyi.jaxrs.auto.mock.mocks.model

import ua.kurinnyi.jaxrs.auto.mock.DependenciesRegistry
import java.lang.reflect.Method
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface ResourceMethodStub {
    fun isMatchingMethod(method:Method, args: Array<Any?>?, dependenciesRegistry: DependenciesRegistry):Boolean
    fun produceResponse(method:Method, args: Array<Any?>?, dependenciesRegistry: DependenciesRegistry):Any?
    fun getStubbedClassName():String
}