package ua.kurinnyi.jaxrs.auto.mock

import java.lang.reflect.Parameter

interface PlatformUtils {
    fun isHttpBody(parameter: Parameter):Boolean
}