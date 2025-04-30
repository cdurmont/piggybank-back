package net.durmont.piggybank.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.Permission;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class PermissionService {

    //TODO security ! Rules to implement:
    // - allow creation of any permission if no Owner permission exists on instance AND instance name = user email
    // - Permission type is capped by user's current maximum permission on this instance :
    //   * max(permission) == 'O' => no limits
    //   * max(permission) == 'W' => limit to 'W' or 'R' + check Account scopes : unlimited if user is 'W' and Account == null, limited to listed Accounts otherwise
    //   * max(permission) == 'R' => creation refused
    public Uni<Permission> create(Permission newPermission) {
        return newPermission.create();
    }

    public Uni<List<Permission>> find(Permission filter, Sort sort, Page page) {
        return Permission.find(filter, sort, page);
    }

    public void findById() {
    }

    public Uni<Permission> update(Permission permission) {
        return permission.update();
    }

    public Uni<Long> delete(Long instanceId, Long id) {
        return Permission.delete(instanceId, id);
    }
}
