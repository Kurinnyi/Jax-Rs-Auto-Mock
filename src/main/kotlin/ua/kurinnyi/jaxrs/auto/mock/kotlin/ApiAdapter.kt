package ua.kurinnyi.jaxrs.auto.mock.kotlin

import org.apache.commons.io.IOUtils
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.ExtractingBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.TemplateEngine
import ua.kurinnyi.jaxrs.auto.mock.httpproxy.RequestProxy
import java.lang.reflect.Method
import javax.servlet.http.HttpServletResponse

class ApiAdapter(
        val method: Method,
        private val response: HttpServletResponse,
        private val defaultJsonBodyProvider:BodyProvider,
        private val defaultExtractingJsonBodyProvider: ExtractingBodyProvider) {

    var shouldFlush = false

    fun <T> getObjectFromJson(bodyProvider:BodyProvider, jsonInfo:String, templateArgs: Map<String, Any>): T {
        val bodyJsonTemplate:String = bodyProvider.provideBodyJson(jsonInfo)
        val bodyRealJson = if (templateArgs.isNotEmpty()){
             TemplateEngine.processTemplate(this.hashCode().toString(), bodyJsonTemplate, templateArgs)
        } else bodyJsonTemplate
        return bodyProvider.provideBodyObjectFromJson(method.returnType, method.genericReturnType, bodyRealJson) as T
    }

    fun <T> getObjectFromJson(jsonInfo:String, templateArgs: Map<String, Any>): T {
        val provider = if (defaultExtractingJsonBodyProvider.canExtract(jsonInfo)) {
            defaultExtractingJsonBodyProvider
        } else defaultJsonBodyProvider
        return getObjectFromJson(provider, jsonInfo, templateArgs)
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