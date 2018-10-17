package ua.kurinnyi.jaxrs.auto.mock

import org.apache.commons.io.IOUtils

import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpServletResponse
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader

class ContextSaveFilter : Filter {

    companion object {
        private val requestHolder = ThreadLocal<RequestWrapper>()
        private val responseHolder = ThreadLocal<HttpServletResponse>()

        val request: HttpServletRequest
            get() = requestHolder.get()
        val response: HttpServletResponse
            get() = responseHolder.get()
    }

    override fun init(filterConfig: FilterConfig) {
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val requestWrapper = RequestWrapper(request as HttpServletRequest)
        requestHolder.set(requestWrapper)
        responseHolder.set(response as HttpServletResponse)
        try {
            chain.doFilter(requestWrapper, response)
        } finally {
            requestHolder.remove()
            responseHolder.remove()
        }
    }

    override fun destroy() {
    }

    class RequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
        private val bytes: ByteArray = IOUtils.toByteArray(request.inputStream)

        override fun getReader(): BufferedReader {
            return BufferedReader(InputStreamReader(ByteArrayInputStream(bytes)))
        }

        override fun getInputStream(): ServletInputStream {
            val byteArrayInputStream = ByteArrayInputStream(bytes)
            return object : ServletInputStream() {
                override fun isFinished(): Boolean {
                    return byteArrayInputStream.available() <= 0
                }

                override fun isReady(): Boolean {
                    return true
                }

                override fun setReadListener(listener: ReadListener) {

                }

                @Throws(IOException::class)
                override fun read(): Int {
                    return byteArrayInputStream.read()
                }

                @Throws(IOException::class)
                override fun close() {
                    byteArrayInputStream.close()
                }
            }
        }
    }


}
