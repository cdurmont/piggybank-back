package net.durmont.piggybank.api.v2;


import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.Instance;
import net.durmont.piggybank.model.Permission;
import net.durmont.piggybank.service.PermissionService;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/api-v2/{instance}/permissions")
public class PermissionResource extends RestResource {

	@Inject
	PermissionService permissionService;

	@GET
	@RolesAllowed("user")
	public Uni<List<Permission>> read(@RestPath("instance") Long instance,
																		@RestQuery("filter") Permission filter,
																		@RestQuery("sort") List<String> sortQuery,
																		@RestQuery("page") @DefaultValue("0") int pageIndex,
																		@RestQuery("size") @DefaultValue("20") int pageSize) {

		return permissionService.find(filter, buildSort(sortQuery), buildPage(pageIndex, pageSize));
	}


	@POST
	@RolesAllowed("user")
	public Uni<Response> create(@RestPath("instance") Long instance, Permission newPermission) {
		newPermission.instance = new Instance();
		newPermission.instance.id = instance;

		return permissionService.create(newPermission)
			.onItem().ifNotNull().transform(inserted -> Response.created(URI.create("/api-v2/" + instance + "/permissions/" + inserted.id)).build())
			.onItem().ifNull().continueWith(Response.status(Response.Status.PRECONDITION_FAILED).build());
	}

	@Path("{id}")
	@PUT
	@RolesAllowed("admin")
	public Uni<Response> update(@RestPath("instance") Long instanceId, Long id, Permission perm) {

		perm.id = id;
		perm.instance = new Instance();
		perm.instance.id = instanceId;
		return permissionService.update(perm)
			.onItem().ifNotNull().transform(permission -> Response.ok(permission).build())
			.onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
	}


	@Path("{id}")
	@DELETE
	@RolesAllowed("admin")
	public Uni<Response> delete(@RestPath("instance") Long instanceId, Long id) {

		return permissionService.delete(instanceId, id)
			.map(result -> result != null && result == 1L ?
				Response.status(Response.Status.NO_CONTENT).build() :
				Response.status(Response.Status.NOT_FOUND).build());
	}
}
