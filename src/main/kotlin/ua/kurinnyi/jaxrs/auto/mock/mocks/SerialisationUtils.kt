package ua.kurinnyi.jaxrs.auto.mock.mocks

import ua.kurinnyi.jaxrs.auto.mock.extensions.ResponseBodyProvider
import ua.kurinnyi.jaxrs.auto.mock.response.TemplateEngine
import java.lang.reflect.Type

class SerialisationUtils(
        val defaultResponseBodyProvider: ResponseBodyProvider,
        private val templateEngine: TemplateEngine) {

    inline fun <reified T> load(responseBodyProvider: ResponseBodyProvider, objectInfo:String, vararg templateArgs: Pair<String, Any>): T {
        return getObjectFromString(responseBodyProvider, objectInfo, templateArgs.toMap(), T::class.java, T::class.java)
    }
    inline fun <reified T> load(objectInfo:String, vararg templateArgs: Pair<String, Any>): T {
        return getObjectFromString(objectInfo, templateArgs.toMap(), T::class.java, T::class.java)
    }
    inline fun <reified T> loadList(responseBodyProvider: ResponseBodyProvider, objectInfo:String, vararg templateArgs: Pair<String, Any>): List<T> {
        return getObjectFromString(responseBodyProvider, objectInfo, templateArgs.toMap(), List::class.java, _listType<T>()) as List<T>
    }

    inline fun <reified T> loadList(objectInfo:String, vararg templateArgs: Pair<String, Any>): List<T> {
        return getObjectFromString(objectInfo, templateArgs.toMap(), List::class.java, _listType<T>()) as List<T>
    }
    inline fun <reified KEY, reified VALUE> loadMap(responseBodyProvider: ResponseBodyProvider, objectInfo:String, vararg templateArgs: Pair<String, Any>): Map<KEY, VALUE> {
        return getObjectFromString(responseBodyProvider, objectInfo, templateArgs.toMap(), Map::class.java, _mapType<KEY, VALUE>()) as Map<KEY, VALUE>
    }

    inline fun <reified KEY, reified VALUE> loadMap(objectInfo:String, vararg templateArgs: Pair<String, Any>): Map<KEY, VALUE> {
        return getObjectFromString(objectInfo, templateArgs.toMap(), Map::class.java, _mapType<KEY, VALUE>()) as Map<KEY, VALUE>
    }

    inline fun  <reified T> toString(value:T): String =
            defaultResponseBodyProvider.objectToString(value, T::class.java, T::class.java)

    inline fun  <reified T> toString(value:List<T>): String =
            defaultResponseBodyProvider.objectToString(value, List::class.java, _listType<T>())

    inline fun  <reified KEY, reified VALUE> toString(value:Map<KEY, VALUE>): String =
            defaultResponseBodyProvider.objectToString(value, Map::class.java, _mapType<KEY, VALUE>())

    fun <T> getObjectFromString(responseBodyProvider: ResponseBodyProvider, objectInfo: String, templateArgs: Map<String, Any>,
                                type: Class<T>, genericType: Type): T {
        val bodyTemplate: String = responseBodyProvider.provideBodyString(objectInfo)
        val bodyProcessedByTemplateEngine = if (templateArgs.isNotEmpty()) {
            templateEngine.processTemplate(bodyTemplate, templateArgs)
        } else bodyTemplate
        return responseBodyProvider.provideBodyObjectFromString(type, genericType, bodyProcessedByTemplateEngine)
    }

    fun <T> getObjectFromString(objectInfo:String, templateArgs: Map<String, Any>, type: Class<T>, genericType: Type): T {
        return getObjectFromString(defaultResponseBodyProvider, objectInfo, templateArgs, type, genericType)
    }
    inline fun <reified KEY, reified VALUE> _mapType(): ParametrizedTypeImpl =
            ParametrizedTypeImpl(arrayOf(KEY::class.java, VALUE::class.java), Map::class.java)

    inline fun <reified T> _listType():ParametrizedTypeImpl =
            ParametrizedTypeImpl(arrayOf(T::class.java), List::class.java)

}