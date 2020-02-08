package ua.kurinnyi.jaxrs.auto.mock

import javax.servlet.http.HttpServletRequest

object Utils {
    fun trimToSingleSpaces(body: String): String {
        return body.trim().replace("\\s+".toRegex(), " ")
    }

    fun bodyAsString(request: HttpServletRequest): String {
        return request.reader.lineSequence().joinToString("\n")
    }

}