package net.durmont.piggybank.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.PaymentMethod;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class PaymentMethodService {

	public Uni<PaymentMethod> create(Long instanceId, PaymentMethod newPM) {
		newPM.setDefaults();
		newPM.instance.id = instanceId;
		return Panache.withTransaction(newPM::persist);
	}

	public Uni<List<PaymentMethod>> list(Long instanceId, PaymentMethod filter) {
		return PaymentMethod.list(instanceId, filter);
	}

	public Uni<PaymentMethod> update(Long instanceId, Long id, PaymentMethod pm) {
		pm.id = id;
		pm.setDefaults();
		return Panache.withTransaction(pm::update);
	}

	public Uni<PaymentMethod> findById(Long instanceId, Long id) {
		return PaymentMethod.<PaymentMethod>findById(id)
			.map(pm -> pm != null && pm.instance != null && Objects.equals(instanceId, pm.instance.id) ? pm : null);
	}

	public Uni<Long> delete(Long instanceId, Long id) {
		return Panache.withTransaction(() -> PaymentMethod.delete(instanceId, id));
	}
}
