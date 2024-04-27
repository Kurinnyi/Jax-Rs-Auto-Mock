package ua.kurinnyi.jaxrs.auto.mock.jersey

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import org.slf4j.LoggerFactory

interface ErrorHandler<T : Throwable> : ExceptionMapper<T> {
    companion object {
        private val logger = LoggerFactory.getLogger(ErrorHandler::class.java)
    }

    fun getMessage(exception: T): String

    override fun toResponse(exception: T): Response {
        val errorMessage = getMessage(exception)
        logger.error(errorMessage)
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(formatErrorJson(errorMessage))
                .build()
    }

    private fun formatErrorJson(message: String) = """
        {
            "error":"$message"
        }""".trimIndent()
}