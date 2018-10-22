package ua.kurinnyi.jaxrs.auto.mock

import javax.ws.rs.NotFoundException
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider


@Provider
class NotFoundExceptionMapper : ErrorHandler<NotFoundException>, ExceptionMapper<NotFoundException>  {

    override fun getMessage(exception: NotFoundException) =
            "There is no resource interface in class path that could match request: ${getRequestUrl()}"

    private fun getRequestUrl() =
            "${ContextSaveFilter.request.method}:${ContextSaveFilter.request.requestURI}${getQueryString()}"

    private fun getQueryString() = ContextSaveFilter.request.queryString?.let { "?$it" } ?: ""
}