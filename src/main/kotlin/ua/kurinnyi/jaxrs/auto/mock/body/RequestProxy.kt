package ua.kurinnyi.jaxrs.auto.mock.body

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.ContextSaveFilter
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


object RequestProxy {

    fun forwardRequest(path: String) {
        val connection = proxyRequest(path)
        proxyResponse(connection)
    }

    private fun proxyRequest(path: String): HttpURLConnection {
        val request = ContextSaveFilter.request
        val connection = formatNewUrl(path, request).openConnection() as HttpURLConnection
        connection.requestMethod = request.method

        copyRequestHeaders(request, connection)

        //conn.setFollowRedirects(false);
        connection.useCaches = false
        connection.doInput = true
        connection.doOutput = true
        connection.connect()
        if (setOf("PUT", "POST").contains(request.method)){
            safeCopy(request.inputStream, connection.outputStream)
        }
        return connection
    }

    private fun proxyResponse(connection: HttpURLConnection) {
        val response = ContextSaveFilter.response
        response.status = connection.responseCode
        copyResponseHeaders(connection, response)

        safeCopy(connection.inputStream, response.outputStream)
        response.flushBuffer()
    }

    private fun formatNewUrl(path: String, request: HttpServletRequest) =
            URL(path + request.requestURI + (request.queryString?.let { "?$it" } ?: ""))

    private fun copyResponseHeaders(connection: HttpURLConnection, response: HttpServletResponse) {
        connection.headerFields
                .filterKeys { it != null }
                .forEach { (key, values) ->
                    values.forEach{ headerValue -> response.setHeader(key, headerValue)}
                }
    }

    private fun copyRequestHeaders(request: HttpServletRequest, connection: HttpURLConnection) {
        request.headerNames.iterator().forEach { headerName ->
            val values = request.getHeaders(headerName)
            values.iterator().forEach { headerValue ->
                connection.addRequestProperty(headerName, headerValue)
            }
        }
    }

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