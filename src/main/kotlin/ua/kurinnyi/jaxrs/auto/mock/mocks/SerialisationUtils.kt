package ua.kurinnyi.jaxrs.auto.mock.mocks

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import ua.kurinnyi.jaxrs.auto.mock.body.BodyProvider
import ua.kurinnyi.jaxrs.auto.mock.body.TemplateEngine
import java.lang.reflect.Type

class SerialisationUtils(
        val defaultBodyProvider: BodyProvider,
        private val templateEngine: TemplateEngine) {

    inline fun <reified T> load(bodyProvider:BodyProvider, objectInfo:String, vararg templateArgs: Pair<String, Any>): T {
        return getObjectFromString(bodyProvider, objectInfo, templateArgs.toMap(), T::class.java, T::class.java)
    }
    inline fun <reified T> load(objectInfo:String, vararg templateArgs: Pair<String, Any>): T {
        return getObjectFromString(objectInfo, templateArgs.toMap(), T::class.java, T::class.java)
    }
    inline fun <reified T> loadList(bodyProvider:BodyProvider, objectInfo:String, vararg templateArgs: Pair<String, Any>): List<T> {
        return getObjectFromString(bodyProvider, objectInfo, templateArgs.toMap(), List::class.java, _listType<T>()) as List<T>
    }

    inline fun <reified T> loadList(objectInfo:String, vararg templateArgs: Pair<String, Any>): List<T> {
        return getObjectFromString(objectInfo, templateArgs.toMap(), List::class.java, _listType<T>()) as List<T>
    }
    inline fun <reified KEY, reified VALUE> loadMap(bodyProvider:BodyProvider, objectInfo:String, vararg templateArgs: Pair<String, Any>): Map<KEY, VALUE> {
        return getObjectFromString(bodyProvider, objectInfo, templateArgs.toMap(), Map::class.java, _mapType<KEY, VALUE>()) as Map<KEY, VALUE>
    }

    inline fun <reified KEY, reified VALUE> loadMap(objectInfo:String, vararg templateArgs: Pair<String, Any>): Map<KEY, VALUE> {
        return getObjectFromString(objectInfo, templateArgs.toMap(), Map::class.java, _mapType<KEY, VALUE>()) as Map<KEY, VALUE>
    }

    inline fun  <reified T> toString(value:T): String =
            defaultBodyProvider.objectToString(value, T::class.java, T::class.java)

    inline fun  <reified T> toString(value:List<T>): String =
            defaultBodyProvider.objectToString(value, List::class.java, _listType<T>())

    inline fun  <reified KEY, reified VALUE> toString(value:Map<KEY, VALUE>): String =
            defaultBodyProvider.objectToString(value, Map::class.java, _mapType<KEY, VALUE>())

    fun <T> getObjectFromString(bodyProvider: BodyProvider, objectInfo: String, templateArgs: Map<String, Any>,
                                type: Class<T>, genericType: Type): T {
        val bodyTemplate: String = bodyProvider.provideBodyString(objectInfo)
        val bodyProcessedByTemplateEngine = if (templateArgs.isNotEmpty()) {
            templateEngine.processTemplate(bodyTemplate, templateArgs)
        } else bodyTemplate
        return bodyProvider.provideBodyObjectFromString(type, genericType, bodyProcessedByTemplateEngine)
    }

    fun <T> getObjectFromString(objectInfo:String, templateArgs: Map<String, Any>, type: Class<T>, genericType: Type): T {
        return getObjectFromString(defaultBodyProvider, objectInfo, templateArgs, type, genericType)
    }

    inline fun <reified KEY, reified VALUE> _mapType():ParameterizedTypeImpl =
            ParameterizedTypeImpl.make(Map::class.java, arrayOf(KEY::class.java, VALUE::class.java), null)

    inline fun <reified T> _listType():ParameterizedTypeImpl =
            ParameterizedTypeImpl.make(List::class.java, arrayOf(T::class.java), null)

}