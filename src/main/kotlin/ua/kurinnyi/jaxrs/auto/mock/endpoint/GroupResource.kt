package ua.kurinnyi.jaxrs.auto.mock.endpoint

import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path("group")
interface GroupResource {

    @GET
    @Path("/")
    @Produces("application/json")
    fun getAll():List<GroupDto>

    @PUT
    @Path("/")
    fun updateList(groups:List<GroupDto>)

}