package ua.kurinnyi.jaxrs.auto.mock.httpproxy

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.ContextSaveFilter
import java.io.InputStream
import java.io.OutputStream
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

object RequestProxy {

    init {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    private val client = ClientBuilder.newClient()

    fun forwardRequest(path: String) {
        proxyRequest(path)
    }

    private fun proxyRequest(path: String) {
        val request = ContextSaveFilter.request
        val requestHeaders = getRequestHeaders(request)
        val url = getUrl(path, request)
        println("Proxying request ${request.method}:$url")
        val builder = client.target(url).request()

        val clientResponse:Response = if (request.method == "GET"){
            requestHeaders.remove("content-length")
            builder.headers(requestHeaders).get()
        } else {
            builder.headers(requestHeaders).method(request.method, Entity.entity(StreamingOutput { stream: OutputStream ->
                safeCopy(request.inputStream, stream)
            }, request.contentType))
        }

        val servletResponse = ContextSaveFilter.response
        servletResponse.status = clientResponse.status
        clientResponse.headers.forEach{ header ->
            header.value.forEach{value -> servletResponse.setHeader(header.key, value.toString())}
        }
        safeCopy(clientResponse.readEntity(InputStream::class.java), servletResponse.outputStream)
        servletResponse.flushBuffer()
    }

    private fun getRequestHeaders(request: HttpServletRequest): MultivaluedMap<String, Any> {
        val headers: MultivaluedMap<String, Any> = MultivaluedHashMap()
        request.headerNames.iterator().forEach { headerName ->
            val values = request.getHeaders(headerName)
            values.iterator().forEach { headerValue ->
                headers.add(headerName, headerValue)
            }
        }
        return headers
    }

    private fun getUrl(path: String, request: HttpServletRequest) =
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