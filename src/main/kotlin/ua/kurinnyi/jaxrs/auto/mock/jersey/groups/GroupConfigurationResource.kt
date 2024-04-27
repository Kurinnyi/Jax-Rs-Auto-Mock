package ua.kurinnyi.jaxrs.auto.mock.jersey.groups

import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces

@Path("group")
interface GroupConfigurationResource {

    @GET
    @Path("/")
    @Produces("application/json")
    fun getAll():List<GroupDto>

    @PUT
    @Path("/")
    fun updateList(groups:List<GroupDto>)

}