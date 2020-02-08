package ua.kurinnyi.jaxrs.auto.mock.mocks

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.TemplateEngine
import java.lang.reflect.Type

class JsonUtils(
        val defaultJsonBodyProvider: BodyProvider,
        private val templateEngine: TemplateEngine) {

    inline fun <reified T> loadJson(bodyProvider:BodyProvider, jsonInfo:String, vararg templateArgs: Pair<String, Any>): T {
        return getObjectFromJson(bodyProvider, jsonInfo, templateArgs.toMap(), T::class.java, T::class.java)
    }
    inline fun <reified T> loadJson(jsonInfo:String, vararg templateArgs: Pair<String, Any>): T {
        return getObjectFromJson(jsonInfo, templateArgs.toMap(), T::class.java, T::class.java)
    }
    inline fun <reified T> loadJsonList(bodyProvider:BodyProvider, jsonInfo:String, vararg templateArgs: Pair<String, Any>): List<T> {
        return getObjectFromJson(bodyProvider, jsonInfo, templateArgs.toMap(), List::class.java, _listType<T>()) as List<T>
    }

    inline fun <reified T> loadJsonList(jsonInfo:String, vararg templateArgs: Pair<String, Any>): List<T> {
        return getObjectFromJson(jsonInfo, templateArgs.toMap(), List::class.java, _listType<T>()) as List<T>
    }
    inline fun <reified KEY, reified VALUE> loadJsonMap(bodyProvider:BodyProvider, jsonInfo:String, vararg templateArgs: Pair<String, Any>): Map<KEY, VALUE> {
        return getObjectFromJson(bodyProvider, jsonInfo, templateArgs.toMap(), Map::class.java, _mapType<KEY, VALUE>()) as Map<KEY, VALUE>
    }

    inline fun <reified KEY, reified VALUE> loadJsonMap(jsonInfo:String, vararg templateArgs: Pair<String, Any>): Map<KEY, VALUE> {
        return getObjectFromJson(jsonInfo, templateArgs.toMap(), Map::class.java, _mapType<KEY, VALUE>()) as Map<KEY, VALUE>
    }

    inline fun  <reified T> toJson(value:T): String =
            defaultJsonBodyProvider.objectToJson(value, T::class.java, T::class.java)

    inline fun  <reified T> toJson(value:List<T>): String =
            defaultJsonBodyProvider.objectToJson(value, List::class.java, _listType<T>())

    inline fun  <reified KEY, reified VALUE> toJson(value:Map<KEY, VALUE>): String =
            defaultJsonBodyProvider.objectToJson(value, Map::class.java, _mapType<KEY, VALUE>())

    fun <T> getObjectFromJson(bodyProvider: BodyProvider, jsonInfo: String, templateArgs: Map<String, Any>,
                              type: Class<T>, genericType: Type): T {
        val bodyJsonTemplate: String = bodyProvider.provideBodyJson(jsonInfo)
        val bodyRealJson = if (templateArgs.isNotEmpty()) {
            templateEngine.processTemplate(bodyJsonTemplate, templateArgs)
        } else bodyJsonTemplate
        return bodyProvider.provideBodyObjectFromJson(type, genericType, bodyRealJson)
    }

    fun <T> getObjectFromJson(jsonInfo:String, templateArgs: Map<String, Any>, type: Class<T>, genericType: Type): T {
        return getObjectFromJson(defaultJsonBodyProvider, jsonInfo, templateArgs, type, genericType)
    }

    inline fun <reified KEY, reified VALUE> _mapType():ParameterizedTypeImpl =
            ParameterizedTypeImpl.make(Map::class.java, arrayOf(KEY::class.java, VALUE::class.java), null)

    inline fun <reified T> _listType():ParameterizedTypeImpl =
            ParameterizedTypeImpl.make(List::class.java, arrayOf(T::class.java), null)

}