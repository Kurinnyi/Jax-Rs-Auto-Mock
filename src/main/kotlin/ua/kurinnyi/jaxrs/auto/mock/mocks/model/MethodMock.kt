package ua.kurinnyi.jaxrs.auto.mock.mocks.model

import ua.kurinnyi.jaxrs.auto.mock.DependenciesRegistry
import java.lang.reflect.Method

interface MethodMock {
    fun isMatchingMethod(method:Method, receivedArguments: Array<Any?>?, dependenciesRegistry: DependenciesRegistry):Boolean
    fun produceResponse(method:Method, receivedArguments: Array<Any?>?, dependenciesRegistry: DependenciesRegistry):Any?
    fun getMockedClassName():String
}