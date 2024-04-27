package ua.kurinnyi.jaxrs.auto.mock.filters

import org.apache.commons.io.IOUtils
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper

class BufferingRequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
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