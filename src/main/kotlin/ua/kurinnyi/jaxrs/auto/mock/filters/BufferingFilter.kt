package ua.kurinnyi.jaxrs.auto.mock.filters

import org.apache.commons.io.IOUtils
import java.io.*
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

class BufferingFilter : Filter {
    override fun destroy() {
    }

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain) {
        chain.doFilter(RequestWrapper(request as HttpServletRequest), ResponseWrapper(response as HttpServletResponse))
    }

    override fun init(filterConfig: FilterConfig?) {
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

    class ResponseWrapper(response: HttpServletResponse) : HttpServletResponseWrapper(response) {

        private val stream = CopyingServletOutputStream(super.getOutputStream())

        fun getResponseBytes():ByteArray  = stream.getResponseBytes()

        override fun getOutputStream(): ServletOutputStream = stream

        class CopyingServletOutputStream(val wrappedStream:ServletOutputStream) : ServletOutputStream() {

            private val copy:ByteArrayOutputStream = ByteArrayOutputStream()

            fun getResponseBytes() = copy.toByteArray()

            override fun isReady(): Boolean = wrappedStream.isReady

            override fun write(b: Int) {
                wrappedStream.write(b)
                copy.write(b)
            }

            override fun setWriteListener(listener: WriteListener?) {
                wrappedStream.setWriteListener(listener)
            }

        }
    }
}