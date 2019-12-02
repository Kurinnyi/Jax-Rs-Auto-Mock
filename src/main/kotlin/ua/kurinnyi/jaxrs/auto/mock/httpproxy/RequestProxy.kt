package ua.kurinnyi.jaxrs.auto.mock.httpproxy

import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.*
import org.apache.http.entity.InputStreamEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import ua.kurinnyi.jaxrs.auto.mock.filters.ContextSaveFilter
import java.io.InputStream
import java.io.OutputStream
import javax.servlet.http.HttpServletRequest


object RequestProxy {

    private val client = HttpClients.createDefault()

    fun forwardRequest(path: String) {
        val servletRequest = ContextSaveFilter.request
        val servletResponse = ContextSaveFilter.response
        val url = formatNewUrl(path, servletRequest)
        println("Proxying request ${servletRequest.method}:$url")
        val httpRequest = getHttpRequest(url, servletRequest)
        addRequestHeaders(servletRequest, httpRequest)
        val response = client.execute(httpRequest)
        servletResponse.status = response.statusLine.statusCode
        response.allHeaders.forEach{ servletResponse.setHeader(it.name, it.value)}
        response.entity?.let { safeCopy(it.content, servletResponse.outputStream) }
        servletResponse.flushBuffer()
    }

    private fun addRequestHeaders(request: HttpServletRequest, httpPost: HttpRequestBase) {
        request.headerNames.iterator().forEach { headerName ->
            if (!setOf("content-length", "transfer-encoding").contains(headerName.toLowerCase())) {
                val values = request.getHeaders(headerName)
                values.iterator().forEach { headerValue ->
                    httpPost.setHeader(BasicHeader(headerName, headerValue))
                }
            }
        }
    }

    private fun getHttpRequest(url: String, request: HttpServletRequest): HttpRequestBase {
        val httpRequest: HttpRequestBase = when (request.method) {
            "POST" -> HttpPost(url)
            "PUT" -> HttpPut(url)
            "DELETE" -> HttpDelete(url)
            "PATCH" -> HttpPatch(url)
            "HEAD" -> HttpHead(url)
            "OPTIONS" -> HttpOptions(url)
            "GET" -> HttpGet(url)
            else -> throw IllegalStateException("Not supported http method ${request.method}")
        }
        if (httpRequest is HttpEntityEnclosingRequestBase){
            httpRequest.entity = InputStreamEntity(request.inputStream)
        }
        return httpRequest
    }

    private fun formatNewUrl(path: String, request: HttpServletRequest) =
            path + request.requestURI + (request.queryString?.let { "?$it" } ?: "")

    private fun safeCopy(inputStream1: InputStream, outputStream1: OutputStream) {
        inputStream1.use { inputStream ->
            outputStream1.use { outputStream ->
                outputStream.flush()
                IOUtils.copy(inputStream, outputStream)
                outputStream.flush()
            }
        }
    }
}