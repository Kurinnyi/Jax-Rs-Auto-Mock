package ua.kurinnyi.jaxrs.auto.mock

import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider


@Provider
class StubNotFoundExceptionMapper : ErrorHandler<StubNotFoundException>, ExceptionMapper<StubNotFoundException> {
    override fun getMessage(exception: StubNotFoundException) = exception.message
}