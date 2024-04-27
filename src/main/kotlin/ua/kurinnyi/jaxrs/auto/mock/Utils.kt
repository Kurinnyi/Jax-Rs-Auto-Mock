package ua.kurinnyi.jaxrs.auto.mock

import java.lang.reflect.Method
import jakarta.servlet.http.HttpServletRequest

object Utils {
    fun trimToSingleSpaces(body: String): String {
        return body.trim().replace("\\s+".toRegex(), " ")
    }

    fun bodyAsString(request: HttpServletRequest): String {
        return request.reader.lineSequence().joinToString("\n")
    }

    fun <T> getReturnValue(method: Method): T? {
        return getReturnValue<T>(method.returnType)
    }

    fun <T> getReturnValue(type: Class<*>?): T {
        val result = when (type) {
            Int::class.java, java.lang.Integer::class.java -> 0
            Long::class.java, java.lang.Long::class.java -> 0L
            Double::class.java, java.lang.Double::class.java -> 0.0
            Float::class.java, java.lang.Float::class.java -> false
            Boolean::class.java, java.lang.Boolean::class.java -> false
            else -> null
        }
        return result as T
    }
}