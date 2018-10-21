package ua.kurinnyi.jaxrs.auto.mock

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider


@Provider
class StubNotFoundExceptionMapper : ExceptionMapper<StubNotFoundException> {

    override fun toResponse(exception: StubNotFoundException): Response {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(exception.message)
                .build()
    }
}