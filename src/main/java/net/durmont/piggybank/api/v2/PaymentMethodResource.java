package net.durmont.piggybank.api.v2;

import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.PaymentMethod;
import net.durmont.piggybank.service.PaymentMethodService;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/api-v2/{instance}/paymentMethods")
public class PaymentMethodResource extends RestResource{

    @Inject
    PaymentMethodService paymentMethodService;

    @POST
    @RolesAllowed("user")
    public Uni<Response> create(@RestPath("instance") Long instance, PaymentMethod newPM) {
        return paymentMethodService.create(instance, newPM)
                .onItem().ifNotNull().transform( inserted -> Response.created(URI.create("/api-v2/"+instance+"/paymentMethods/"+inserted.id)).build() )
                .onItem().ifNull().continueWith(Response.ok().status(Response.Status.PRECONDITION_FAILED).build());
    }

    @GET
    @RolesAllowed("user")
    public Uni<List<PaymentMethod>> read(@RestPath("instance") Long instance,
                                   @RestQuery("filter") PaymentMethod filter,
                                   @RestQuery("sort") List<String> sortQuery,
                                   @RestQuery("page") @DefaultValue("0") int pageIndex,
                                   @RestQuery("size") @DefaultValue("200") int pageSize) {

        return paymentMethodService.list(instance, filter);
    }

    @Path("{id}")
    @PUT
    @RolesAllowed("user")
    public Uni<Response> update(@RestPath("instance") Long instance, Long id, PaymentMethod pm) {
        return paymentMethodService.update(instance, id, pm)
                .onItem().ifNotNull().transform(acc -> Response.ok(acc).build())
                .onItem().ifNull().continueWith(Response.ok().status(Response.Status.NOT_FOUND).build());
    }

    @Path("{id}")
    @DELETE
    @RolesAllowed("user")
    public Uni<Response> delete(@RestPath("instance") Long instance, Long id) {
        return paymentMethodService.delete(instance, id)
                .map(result -> result != null && result == 1L ?
                        Response.ok().status(Response.Status.NO_CONTENT).build() :
                        Response.ok().status(Response.Status.NOT_FOUND).build());
    }

}
