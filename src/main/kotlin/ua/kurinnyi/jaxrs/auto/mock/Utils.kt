package ua.kurinnyi.jaxrs.auto.mock

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
}