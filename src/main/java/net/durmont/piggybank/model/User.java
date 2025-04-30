package net.durmont.piggybank.model;


import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import org.apache.commons.beanutils.BeanUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "PiggyUser")
public class User extends ConvertedEntity {

	public Boolean admin;
	public String login;
	public String name;
	public String mongoId;

	public User() {
	}

	public User(String json) {
		super(json);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		User user = (User) o;

		if (admin != user.admin) return false;
		if (!Objects.equals(login, user.login)) return false;
		if (!Objects.equals(name, user.name)) return false;
		return Objects.equals(mongoId, user.mongoId);
	}

	@Override
	public int hashCode() {
		int result = ((admin != null && admin == Boolean.TRUE) ? 1 : 0);
		result = 31 * result + (login != null ? login.hashCode() : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (mongoId != null ? mongoId.hashCode() : 0);
		return result;
	}


	@Override
	public void setDefaults() {
		admin = false;
	}

	public Uni<User> create() {
		return Panache.withTransaction(this::persist);
	}

	public Uni<User> update() {
		return Panache.withTransaction(
			() -> User.<User>findById(id)
				.map(instanceDb -> {
					try {
						BeanUtils.copyProperties(instanceDb, this);
					} catch (IllegalAccessException | InvocationTargetException e) {
						Log.error("Error copying new properties of Instance " + id, e);
					}
					return instanceDb;
				}));
	}

	public static Uni<List<User>> find(User filter, Sort sort, Page page) {
		Map<String, Object> params = new HashMap<>();
		String query = "1=1";
		if (filter != null) {
			if (filter.admin != null) {
				query += " AND admin=:admin";
				params.put("admin", filter.admin);
			}
			if (filter.login != null) {
				query += " AND login=:login";
				params.put("login", filter.login);
			}
			if (filter.name != null) {
				query += " AND name=:name";
				params.put("name", filter.name);
			}
		}
		return User.find(query, sort, params)
			.page(page)
			.list();
	}

	public static Uni<Boolean> delete(Long id) {
		return Panache.withTransaction(() -> User.deleteById(id));
	}
}
