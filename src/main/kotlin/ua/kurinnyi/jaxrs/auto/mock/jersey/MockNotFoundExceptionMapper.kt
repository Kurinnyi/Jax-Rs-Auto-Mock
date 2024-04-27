package ua.kurinnyi.jaxrs.auto.mock.jersey

import ua.kurinnyi.jaxrs.auto.mock.MockNotFoundException
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider


@Provider
class MockNotFoundExceptionMapper : ErrorHandler<MockNotFoundException>, ExceptionMapper<MockNotFoundException> {
    override fun getMessage(exception: MockNotFoundException) = exception.message
}