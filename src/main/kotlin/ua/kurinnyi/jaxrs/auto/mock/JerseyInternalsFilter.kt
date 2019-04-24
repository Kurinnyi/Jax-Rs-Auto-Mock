package ua.kurinnyi.jaxrs.auto.mock

import org.apache.commons.io.IOUtils
import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap
import org.glassfish.jersey.message.MessageBodyWorkers
import org.glassfish.jersey.server.ContainerRequest
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.MediaType

class JerseyInternalsFilter : ContainerRequestFilter {

    companion object {
        private lateinit var workers: MessageBodyWorkers

        fun <T> prepareResponse(type:Class<T>, genericType: Type, body: String): T {
            val inputStream = IOUtils.toInputStream(body)
            val bodyReader = workers.getMessageBodyReader(type, type, emptyArray(), MediaType.APPLICATION_JSON_TYPE, null)
            return bodyReader.readFrom(type, genericType, emptyArray(), MediaType.APPLICATION_JSON_TYPE,
                    ImmutableMultivaluedMap.empty(), inputStream)
        }

        fun <T> toJson(value: T, type:Class<T>, genericType: Type): String {
            val outputStream = ByteArrayOutputStream()
            val bodyWriter = workers.getMessageBodyWriter(type, type, emptyArray(), MediaType.APPLICATION_JSON_TYPE, null)
            bodyWriter.writeTo(value, type, genericType, emptyArray(), MediaType.APPLICATION_JSON_TYPE,
                    ImmutableMultivaluedMap.empty(), outputStream)
            return String(outputStream.toByteArray())
        }
    }

    override fun filter(requestContext: ContainerRequestContext) {
        workers = (requestContext as ContainerRequest).workers
    }

}

