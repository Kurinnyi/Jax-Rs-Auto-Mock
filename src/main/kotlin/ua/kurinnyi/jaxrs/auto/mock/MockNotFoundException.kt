package ua.kurinnyi.jaxrs.auto.mock

import java.lang.RuntimeException

class MockNotFoundException(override val message:String) : RuntimeException()