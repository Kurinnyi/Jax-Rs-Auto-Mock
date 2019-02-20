package ua.kurinnyi.jaxrs.auto.mock.kotlin

import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.ExtractingBodyProvider
import java.lang.reflect.Method
import javax.servlet.http.HttpServletResponse

object ApiAdapterFactory {
    lateinit var defaultBodyProvider:BodyProvider
    lateinit var defaultExtractingBodyProvider:ExtractingBodyProvider

    fun getApiAdapter(method: Method, response: HttpServletResponse):ApiAdapter {
        return ApiAdapter(method, response, defaultBodyProvider, defaultExtractingBodyProvider)
    }
}