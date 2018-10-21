package ua.kurinnyi.jaxrs.auto.mock

import java.lang.RuntimeException

class StubNotFoundException(override val message:String) : RuntimeException()