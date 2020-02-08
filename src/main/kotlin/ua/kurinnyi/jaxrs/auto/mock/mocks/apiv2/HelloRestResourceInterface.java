package ua.kurinnyi.jaxrs.auto.mock.mocks.apiv2;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/helloworld")
public interface HelloRestResourceInterface {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public String getHello(@QueryParam("hi") String hello, @PathParam("id") long id);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("echo/{id}")
    public long echo(@PathParam("id") long id);

}

