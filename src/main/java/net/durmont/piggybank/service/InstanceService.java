package net.durmont.piggybank.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.Instance;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class InstanceService {

	public Uni<List<Instance>> read(Instance filter, Sort sort, Page page) {
		return Instance.findAll(sort).page(page).list();
	}

	public Uni<Instance> readOne(Long id) {
		return Instance.findById(id);
	}

	public Uni<Instance> create(Instance newInstance) {
		return Panache.withTransaction(newInstance::persist);
	}

	public Uni<Instance> patch(Long id, Instance updated) {
		updated.id = id;
		return Panache.withTransaction(updated::update);
	}

	public Uni<Boolean> delete(Long id) {
		return Panache.withTransaction(() -> Instance.deleteById(id));
	}
}
