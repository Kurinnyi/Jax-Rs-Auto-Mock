package ua.kurinnyi.jaxrs.auto.mock

import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider


@Provider
class NotFoundExceptionMapper : ExceptionMapper<NotFoundException> {

    override fun toResponse(exception: NotFoundException): Response {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("There is no resource interface in class path that could match request.\n " +
                        "${ContextSaveFilter.request.method}:${ContextSaveFilter.request.requestURI}" +
                        (ContextSaveFilter.request.queryString?.let { "?$it" }?:""))
                .build()
    }
}