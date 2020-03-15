package ua.kurinnyi.jaxrs.auto.mock.filters

import java.io.ByteArrayOutputStream
import javax.servlet.ServletOutputStream
import javax.servlet.WriteListener
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

class BufferingResponseWrapper(response: HttpServletResponse) : HttpServletResponseWrapper(response) {

    private val stream = CopyingServletOutputStream(super.getOutputStream())

    fun getResponseBytes():ByteArray  = stream.getResponseBytes()

    override fun getOutputStream(): ServletOutputStream = stream

    class CopyingServletOutputStream(val wrappedStream: ServletOutputStream) : ServletOutputStream() {

        private val copy: ByteArrayOutputStream = ByteArrayOutputStream()

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