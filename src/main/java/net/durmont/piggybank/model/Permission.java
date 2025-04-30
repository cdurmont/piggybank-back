package net.durmont.piggybank.model;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import org.apache.commons.beanutils.BeanUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
public class Permission extends ConvertedEntity {

	@ManyToOne(targetEntity = Instance.class)
	public Instance instance;
	@ManyToOne(targetEntity = User.class)
	public User user;
	@ManyToOne(targetEntity = Account.class)
	public Account account;
	@Column(length = 1)
	public String type;
	public String mongoId;

	public Permission() {
	}

	public Permission(String json) {
		super(json);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Permission)) return false;

		Permission that = (Permission) o;

		if (!Objects.equals(user, that.user)) return false;
		if (!Objects.equals(account, that.account)) return false;
		if (!Objects.equals(type, that.type)) return false;
		return Objects.equals(mongoId, that.mongoId);
	}

	@Override
	public int hashCode() {
		int result = user != null ? user.hashCode() : 0;
		result = 31 * result + (account != null ? account.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (mongoId != null ? mongoId.hashCode() : 0);
		return result;
	}


	@Override
	public void setDefaults() {

	}

	public static Uni<List<Permission>> find(Permission filter, Sort sort, Page page) {
		Map<String, Object> params = new HashMap<>();
		String query = "1=1";
		if (filter != null) {
			if (filter.type != null) {
				query += " AND type=:type";
				params.put("type", filter.type);
			}
			if (filter.user != null) {
				query += " AND user.id=:user_id";
				params.put("user_id", filter.user.id);
			}
			if (filter.account != null) {
				query += " AND account.id=:account_id";
				params.put("account_id", filter.account.id);
			}
		}
		return Permission.find(query, sort, params)
			.page(page)
			.list();
	}

	public Uni<Permission> update() {
		return Panache.withTransaction(
			() -> Permission.<Permission>findById(id)
				.map(permissionDb -> {
					// isolate instances
					if (Objects.equals(permissionDb.instance.id, instance.id)) {
						// no cheating ! Prevent malicious update of instance id
						instance = permissionDb.instance;
						try {
							BeanUtils.copyProperties(permissionDb, this);
						} catch (IllegalAccessException | InvocationTargetException e) {
							Log.error("Error copying new properties of Permission " + id, e);
						}
					}
					return permissionDb;
				}));
	}

	public Uni<Permission> create() {
		return Panache.withTransaction(this::persist);
	}

	public static Uni<Long> delete(Long instanceId, Long id) {
		return Panache.withTransaction(
			() -> Permission.delete(
				"instance.id=:instance_id and id=:id",
				Parameters.with("instance_id", instanceId).and("id", id))
		);
	}
}
