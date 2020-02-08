package ua.kurinnyi.jaxrs.auto.mock

import java.lang.reflect.Method
import java.lang.reflect.Parameter
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam

object Utils {
    fun trimToSingleSpaces(body: String): String {
        return body.trim().replace("\\s+".toRegex(), " ")
    }

    fun bodyAsString(request: HttpServletRequest): String {
        return request.reader.lineSequence().joinToString("\n")
    }

    fun isHttpBody(parameter: Parameter) =
            parameter.annotations.none { it is QueryParam || it is PathParam }

    fun <T> getReturnValue(method: Method): T? {
        val result =  when (method.returnType) {
            Int::class.java -> 0
            Long::class.java -> 0L
            Double::class.java -> 0.0
            Float::class.java -> false
            Boolean::class.java -> false
            else -> null
        }
        return result as T?
    }
}