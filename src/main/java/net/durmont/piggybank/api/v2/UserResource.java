package net.durmont.piggybank.api.v2;

import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.User;
import net.durmont.piggybank.service.UserService;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/api-v2/users")
public class UserResource extends RestResource {

	@Inject
	UserService userService;

	@GET
	@RolesAllowed("user")
	public Uni<List<User>> read(@RestQuery("filter") User filter,
															@RestQuery("sort") List<String> sortQuery,
															@RestQuery("page") @DefaultValue("0") int pageIndex,
															@RestQuery("size") @DefaultValue("20") int pageSize) {

		return userService.find(filter, buildSort(sortQuery), buildPage(pageIndex, pageSize));
	}


	@Path("{id}")
	@GET
	@RolesAllowed("user")
	public Uni<Response> readOne(@RestPath("id") Long id) {

		return userService.findById(id)
			.onItem().ifNotNull().transform(user -> Response.ok(user).build())
			.onItem().ifNull().continueWith(() -> Response.status(Response.Status.NOT_FOUND).build());
	}

	@POST
	@RolesAllowed("user")
	public Uni<Response> create(User newUser) {

		return userService.create(newUser)
			.onItem().ifNotNull().transform(inserted -> Response.created(URI.create("/api-v2/users/" + inserted.id)).entity(inserted).build())
			.onItem().ifNull().continueWith(Response.status(Response.Status.PRECONDITION_FAILED).build());
	}

	@Path("{id}")
	@PUT
	@RolesAllowed("user")
	public Uni<Response> update(Long id, User user) {

		user.id = id;
		return userService.update(user)
			.onItem().ifNotNull().transform(inst -> Response.ok(inst).build())
			.onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
	}

	@Path("{id}")
	@DELETE
	@RolesAllowed("user")
	public Uni<Response> delete(Long id) {

		return userService.delete(id)
			.map(result -> Boolean.TRUE.equals(result) ?
				Response.status(Response.Status.NO_CONTENT).build() :
				Response.status(Response.Status.NOT_FOUND).build());
	}

}
