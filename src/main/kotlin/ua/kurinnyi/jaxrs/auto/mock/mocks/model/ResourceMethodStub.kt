package ua.kurinnyi.jaxrs.auto.mock.mocks.model

import java.lang.reflect.Method
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface ResourceMethodStub {
    fun isMatchingMethod(method:Method, args: Array<Any?>?, request:HttpServletRequest):Boolean
    fun produceResponse(method:Method, args: Array<Any?>?, response: HttpServletResponse):Any?
    fun getStubbedClassName():String
}