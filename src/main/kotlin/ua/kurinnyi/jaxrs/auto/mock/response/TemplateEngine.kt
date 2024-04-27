package ua.kurinnyi.jaxrs.auto.mock.response

import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.StringLoader
import java.io.StringWriter


class TemplateEngine {

    private val engine = PebbleEngine.Builder().loader(StringLoader()).build()

    fun processTemplate(templateBody:String, arguments:Any): String {
        val context: Map<String, Any?> =
                if (arguments is Map<*, *>)
                    arguments.mapKeys { (key, _) -> key.toString() }
                else mapOf("args" to arguments)

        val writer = StringWriter()
        engine.getTemplate(templateBody).evaluate(writer, context)
        return writer.toString()
    }
}