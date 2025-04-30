package net.durmont.piggybank.api.v2;

import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.Entry;
import net.durmont.piggybank.model.Instance;
import net.durmont.piggybank.service.EntryService;
import org.jboss.resteasy.reactive.RestPath;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/api-v2/{instance}/entries")
public class EntryResource extends RestResource {

    @Inject
    EntryService entryService;

    @POST
    @RolesAllowed("user")
    public Uni<Response> create(@RestPath("instance") Long instance, Entry newEntry) {
        newEntry.instance = new Instance();
        newEntry.instance.id = instance;
        return entryService.create(instance, newEntry)
                .onItem().ifNotNull().transform( inserted -> Response.created(URI.create("/api-v2/"+instance+"/entries/"+inserted.id)).build() )
                .onItem().ifNull().continueWith(Response.ok().status(Response.Status.PRECONDITION_FAILED).build());
    }


    @Path("{id}")
    @PUT
    @RolesAllowed("user")
    public Uni<Response> update(@RestPath("instance") Long instance, Long id, Entry entry) {
        return entryService.update(instance, id, entry)
                .onItem().ifNotNull().transform(acc -> Response.ok(acc).build())
                .onItem().ifNull().continueWith(Response.ok().status(Response.Status.NOT_FOUND).build());
    }

    @Path("{id}")
    @DELETE
    @RolesAllowed("user")
    public Uni<Response> delete(@RestPath("instance") Long instance, Long id) {
        return entryService.delete(instance, id)
                .map(result -> result != null && result == 1L ?
                        Response.ok().status(Response.Status.NO_CONTENT).build() :
                        Response.ok().status(Response.Status.NOT_FOUND).build());
    }


}
