package example.contract;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/yaml")
public interface YamlRestResourceInterface {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/yaml")
    public Dto getDto(@QueryParam("hi") String hi);

}

