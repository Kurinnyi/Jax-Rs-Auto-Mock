package example

import ua.kurinnyi.jaxrs.auto.mock.StubServer

object Main {
    @JvmStatic
    fun main(args: Array<String>) = StubServer().setPort(8080).start()
}