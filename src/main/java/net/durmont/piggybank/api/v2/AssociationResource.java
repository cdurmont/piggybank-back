package net.durmont.piggybank.api.v2;

import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import net.durmont.piggybank.model.Association;
import net.durmont.piggybank.service.AssociationService;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.inject.Inject;
import java.net.URI;
import java.util.List;

@Path("/api-v2/{instance}/associations")
public class AssociationResource extends RestResource {

    @Inject
    AssociationService associationService;


    @POST
    @RolesAllowed("user")
    public Uni<Response> create(@RestPath("instance") Long instance, Association newAssociation) {
        return associationService.create(instance, newAssociation)
                .onItem().ifNotNull().transform( inserted -> Response.created(URI.create("/api-v2/"+instance+"/associations/"+inserted.id)).build() )
                .onItem().ifNull().continueWith(Response.ok().status(Response.Status.PRECONDITION_FAILED).build());
    }

    @GET
    @RolesAllowed("user")
    public Uni<List<Association>> read(@RestPath("instance") Long instance,
                                   @RestQuery("filter") Association filter,
                                   @RestQuery("sort") List<String> sortQuery,
                                   @RestQuery("page") @DefaultValue("0") int pageIndex,
                                   @RestQuery("size") @DefaultValue("20") int pageSize) {

        Sort sort = buildSort(sortQuery);
        if (sort.getColumns().isEmpty())
            sort.and("id");
        return associationService.list(instance, filter, sort, buildPage(pageIndex, pageSize));
    }

    @Path("{id}")
    @PUT
    @RolesAllowed("user")
    public Uni<Response> update(@RestPath("instance") Long instance, Long id, Association association) {
        return associationService.update(instance, id, association)
                .onItem().ifNotNull().transform(acc -> Response.ok(acc).build())
                .onItem().ifNull().continueWith(Response.ok().status(Response.Status.NOT_FOUND).build());
    }

    @Path("{id}")
    @DELETE
    @RolesAllowed("user")
    public Uni<Response> delete(@RestPath("instance") Long instance, Long id) {
        return associationService.delete(instance, id)
                .map(result -> result != null && result == 1L ?
                        Response.ok().status(Response.Status.NO_CONTENT).build() :
                        Response.ok().status(Response.Status.NOT_FOUND).build());
    }

}
