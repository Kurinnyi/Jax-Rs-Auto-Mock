package ua.kurinnyi.jaxrs.auto.mock.filters

import java.io.ByteArrayOutputStream
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper

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