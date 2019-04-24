package ua.kurinnyi.jaxrs.auto.mock.kotlin

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import ua.kurinnyi.jaxrs.auto.mock.JerseyInternalsFilter
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.ExtractingBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.TemplateEngine
import java.lang.reflect.Type

object JsonUtils {

    lateinit var defaultJsonBodyProvider: BodyProvider
    lateinit var defaultExtractingJsonBodyProvider: ExtractingBodyProvider

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
        JerseyInternalsFilter.toJson(value, T::class.java, T::class.java)

    inline fun  <reified T> toJson(value:List<T>): String =
        JerseyInternalsFilter.toJson(value, List::class.java, _listType<T>())

    inline fun  <reified KEY, reified VALUE> toJson(value:Map<KEY, VALUE>): String =
        JerseyInternalsFilter.toJson(value, Map::class.java, _mapType<KEY, VALUE>())

    fun <T> getObjectFromJson(bodyProvider: BodyProvider, jsonInfo: String, templateArgs: Map<String, Any>,
                              type: Class<T>, genericType: Type): T {
        val bodyJsonTemplate: String = bodyProvider.provideBodyJson(jsonInfo)
        val bodyRealJson = if (templateArgs.isNotEmpty()) {
            TemplateEngine.processTemplate(this.hashCode().toString(), bodyJsonTemplate, templateArgs)
        } else bodyJsonTemplate
        return bodyProvider.provideBodyObjectFromJson(type, genericType, bodyRealJson)
    }

    fun <T> getObjectFromJson(jsonInfo:String, templateArgs: Map<String, Any>, type: Class<T>, genericType: Type): T {
        val provider = if (defaultExtractingJsonBodyProvider.canExtract(jsonInfo)) {
            defaultExtractingJsonBodyProvider
        } else defaultJsonBodyProvider
        return getObjectFromJson(provider, jsonInfo, templateArgs, type, genericType)
    }


    inline fun <reified KEY, reified VALUE> _mapType() =
            ParameterizedTypeImpl.make(Map::class.java, arrayOf(KEY::class.java, VALUE::class.java), null)

    inline fun <reified T> _listType() = ParameterizedTypeImpl.make(List::class.java, arrayOf(T::class.java), null)

}