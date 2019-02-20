package ua.kurinnyi.jaxrs.auto.mock.kotlin

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.TemplateEngine
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.RequestProxy
import java.lang.reflect.Method
import javax.servlet.http.HttpServletResponse

class ApiAdapter(val method: Method, private val response: HttpServletResponse) {

    var shouldFlush = false

    fun <T> getObjectFromJson(bodyProvider:BodyProvider, jsonInfo:String): T {
        return bodyProvider.provideBodyObject(method.returnType, method.genericReturnType, jsonInfo) as T
    }

    fun <T> getObjectFromJsonTemplate(bodyProvider:BodyProvider, jsonInfo:String, templateArgs: Map<String, Any>): T {
        val bodyJsonTemplate:String = bodyProvider.provideBodyJson(jsonInfo)
        val bodyRealJson = TemplateEngine.processTemplate(this.hashCode().toString(), bodyJsonTemplate, templateArgs)
        return bodyProvider.provideBodyObjectFromJson(method.returnType, method.genericReturnType, bodyRealJson) as T
    }

    fun code(code: Int) {
        response.status = code
        shouldFlush = true
    }

    fun bodyRaw(body: String) {
        IOUtils.write(body, response.writer)
        shouldFlush = true
    }

    fun proxyTo(path: String) {
        RequestProxy.forwardRequest(path)
    }

    fun header(headerName: String, headerValue: String) {
        response.addHeader(headerName, headerValue)
    }

}