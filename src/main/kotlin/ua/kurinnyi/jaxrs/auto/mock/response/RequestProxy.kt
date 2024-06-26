package ua.kurinnyi.jaxrs.auto.mock.response

import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.*
import org.apache.http.entity.InputStreamEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import ua.kurinnyi.jaxrs.auto.mock.filters.HttpRequestResponseHolder
import java.io.InputStream
import java.io.OutputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse


class RequestProxy(private val httpRequestResponseHolder: HttpRequestResponseHolder) {

    private val client = HttpClients.createDefault()

    fun forwardRequest(path: String) {
        val servletRequest = httpRequestResponseHolder.request
        val servletResponse = httpRequestResponseHolder.response
        val url = formatNewUrl(path, servletRequest)
        println("Proxying request ${servletRequest.method}:$url")
        val httpRequest = getHttpRequest(url, servletRequest)
        addRequestHeaders(servletRequest, httpRequest)
        val response = client.execute(httpRequest)
        servletResponse.status = response.statusLine.statusCode
        addResponseHeaders(response, servletResponse)
        response.entity?.let { safeCopy(it.content, servletResponse.outputStream) }
        servletResponse.flushBuffer()
    }

    private fun addResponseHeaders(response: CloseableHttpResponse, servletResponse: HttpServletResponse) {
        response.allHeaders
                .filterNot { it.name.toLowerCase() in setOf("transfer-encoding", "date") }
                .forEach { servletResponse.setHeader(it.name, it.value) }
    }

    private fun addRequestHeaders(request: HttpServletRequest, httpPost: HttpRequestBase) {
        request.headerNames.iterator().forEach { headerName ->
            if (headerName.toLowerCase() !in setOf("content-length", "transfer-encoding")) {
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