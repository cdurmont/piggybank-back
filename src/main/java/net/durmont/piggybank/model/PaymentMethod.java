package net.durmont.piggybank.model;

import io.quarkus.logging.Log;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import org.apache.commons.beanutils.BeanUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
public class PaymentMethod extends ConvertedEntity {

	@ManyToOne(targetEntity = Instance.class)
	public Instance instance;
	@ManyToOne(targetEntity = User.class)
	public User user;
	@ManyToOne(targetEntity = Account.class)
	public Account account;
	public String name;

	public PaymentMethod() {
	}

	public PaymentMethod(String json) {
		super(json);
	}

	public static Uni<List<PaymentMethod>> list(Long instanceId, PaymentMethod filter) {
		Map<String, Object> params = new HashMap<>();
		String query = "1=1";
		query += " AND instance.id=:instance_id";
		params.put("instance_id", instanceId);
		if (filter != null) {
			if (filter.id != null) {
				query += " AND id=:id";
				params.put("id", filter.id);
			}
			if (filter.user != null) {
				query += " AND user=:user";
				params.put("user", filter.user);
			}
			if (filter.account != null) {
				query += " AND account=:account";
				params.put("account", filter.account);
			}
			if (filter.name != null) {
				query += " AND name=:name";
				params.put("name", filter.name);
			}
		}
		return PaymentMethod.find(query, params).list();
	}

	public static Uni<Long> delete(Long instanceId, Long id) {
		return PaymentMethod.delete("instance.id=:instance_id and id=:id", Parameters.with("instance_id", instanceId).and("id", id));
	}

	@Override
	public void setDefaults() {
		if (instance == null)
			instance = new Instance(1L);
		if (name == null)
			name = "(sans nom)";
	}

	public Uni<PaymentMethod> update() {
		return PaymentMethod.<PaymentMethod>findById(id)
			.map(pmDb -> {
				if (pmDb == null || (pmDb.instance != null && !Objects.equals(instance.id, pmDb.instance.id)))
					return null;
				instance = pmDb.instance;
				try {
					BeanUtils.copyProperties(pmDb, this);
				} catch (IllegalAccessException | InvocationTargetException e) {
					Log.error("Error copying new properties of PaymentMethod " + id, e);
				}
				return pmDb;
			});
	}
}
