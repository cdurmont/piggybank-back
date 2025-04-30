package net.durmont.piggybank.api.v2;

import com.fasterxml.jackson.annotation.JsonView;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.Views;
import net.durmont.piggybank.model.Instance;
import net.durmont.piggybank.model.Transaction;
import net.durmont.piggybank.service.TransactionService;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/api-v2/{instance}/transactions")
public class TransactionResource extends RestResource{

    @Inject
    TransactionService transactionService;

    @POST
    @RolesAllowed("user")
    public Uni<Response> create(@RestPath("instance") Long instance, Transaction newTxn) {
        newTxn.instance = new Instance();
        newTxn.instance.id = instance;
        return transactionService.create(instance, newTxn)
                .onItem().ifNotNull().transform( inserted -> Response.created(URI.create("/api-v2/"+instance+"/transactions/"+inserted.id)).build() )
                .onItem().ifNull().continueWith(Response.ok().status(Response.Status.PRECONDITION_FAILED).build());
    }

    @GET
    @RolesAllowed("user")
    @JsonView(Views.IncludeOneToMany.class)
    public Uni<List<Transaction>> read(@RestPath("instance") Long instance,
                                   @RestQuery("filter") Transaction filter,
                                   @RestQuery("sort") List<String> sortQuery,
                                   @RestQuery("page") @DefaultValue("0") int pageIndex,
                                   @RestQuery("size") @DefaultValue("1000") int pageSize) {

        return transactionService.list(instance, filter, buildSort(sortQuery), buildPage(pageIndex, pageSize));
    }



    @Path("{id}")
    @GET
    @RolesAllowed("user")
    @JsonView(Views.IncludeOneToMany.class)
    public Uni<Response> readOne(@RestPath("instance") Long instance,
                                 @RestPath("id") Long id) {
        return transactionService.findById(instance, id)
                .onItem().ifNotNull().transform(txn -> Response.ok(txn).status(Response.Status.OK).build())
                .onItem().ifNull().continueWith(() -> Response.ok().status(Response.Status.NOT_FOUND).build());
    }

    @Path("{id}")
    @PUT
    @RolesAllowed("user")
    public Uni<Response> update(@RestPath("instance") Long instance, Long id, Transaction transaction) {
        return transactionService.update(instance, id, transaction)
                .onItem().ifNotNull().transform(txn -> Response.ok(txn).build())
                .onItem().ifNull().continueWith(Response.ok().status(Response.Status.NOT_FOUND).build());
    }

    @Path("{id}")
    @DELETE
    @RolesAllowed("user")
    public Uni<Response> delete(@RestPath("instance") Long instance, Long id) {
        return transactionService.delete(instance, id)
                .map(result -> result != null && result == 1L ?
                        Response.ok().status(Response.Status.NO_CONTENT).build() :
                        Response.ok().status(Response.Status.NOT_FOUND).build());
    }
}
