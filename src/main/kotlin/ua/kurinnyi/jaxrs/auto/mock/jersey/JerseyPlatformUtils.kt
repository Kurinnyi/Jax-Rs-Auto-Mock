package ua.kurinnyi.jaxrs.auto.mock.jersey

import ua.kurinnyi.jaxrs.auto.mock.PlatformUtils
import java.lang.reflect.Parameter
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam

class JerseyPlatformUtils : PlatformUtils {
    override fun isHttpBody(parameter: Parameter): Boolean {
        return parameter.annotations.none { it is QueryParam || it is PathParam }
    }
}