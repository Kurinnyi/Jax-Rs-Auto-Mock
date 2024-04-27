package ua.kurinnyi.jaxrs.auto.mock.jersey

import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider


@Provider
class NotFoundExceptionMapper : ErrorHandler<NotFoundException>, ExceptionMapper<NotFoundException>  {

    override fun getMessage(exception: NotFoundException) =
            "There is no resource interface in class path that could match request: ${getRequestUrl()}"

    private fun getRequestUrl(): String {
        val request = JerseyDependenciesRegistry.httpRequestResponseHolder().request
        return "${request.method}:${request.requestURI}${getQueryString()}"
    }

    private fun getQueryString() = JerseyDependenciesRegistry.httpRequestResponseHolder().request.queryString?.let { "?$it" } ?: ""
}