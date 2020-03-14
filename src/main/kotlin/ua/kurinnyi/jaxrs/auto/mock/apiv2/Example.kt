package ua.kurinnyi.jaxrs.auto.mock.apiv2

class Example : Mock<HelloRestResourceInterface>({ mock ->

    bypassAnyNotMatched("/toPath")
    recordAnyBypassed()
    priority(10)

    val word = capture<String>()
    mock.getHello(word(notNull()), match { it > 1 })
            .header("hello", "setResponseHeader")
            .header("hello", "setResponseHeader")
            .respond {
                header("response", "setResponseHeader")
                code(123)
                bodyJson("Hello", "word" to word())
            }
})