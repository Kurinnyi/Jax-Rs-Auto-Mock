package ua.kurinnyi.jaxrs.auto.mock

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper

interface ErrorHandler<T : Throwable> : ExceptionMapper<T> {

    fun getMessage(exception: T): String

    override fun toResponse(exception: T): Response {
        val errorMessage = getMessage(exception)
        System.err.println(errorMessage)
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(formatErrorJson(errorMessage))
                .build()
    }

    private fun formatErrorJson(message: String) = """
        {
            "error":"$message"
        }""".trimIndent()
}