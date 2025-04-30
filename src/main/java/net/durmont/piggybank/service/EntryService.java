package net.durmont.piggybank.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.Entry;
import net.durmont.piggybank.model.Instance;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class EntryService {

	public Uni<Entry> create(Long instanceId, Entry newEntry) {
		newEntry.setDefaults();
		if (newEntry.instance == null)
			newEntry.instance = new Instance();
		newEntry.instance.id = instanceId;

		return Panache.withTransaction(newEntry::persist);
	}

	public Uni<List<Entry>> list(Long instanceId, Entry filter, Sort sort, Page page) {
		return Entry.list(instanceId, filter, sort, page);
	}

	public Uni<Entry> findById(Long instanceId, Long id) {
		// will return the transaction UNLESS it is not from the given instance
		return Entry.<Entry>findById(id)
			.map(entry -> entry != null && entry.instance != null && Objects.equals(instanceId, entry.instance.id) ? entry : null);
	}

	public Uni<Entry> update(Long instanceId, Long id, Entry entry) {
		entry.id = id;
		entry.setDefaults();
		if (entry.instance == null)
			entry.instance = new Instance();
		entry.instance.id = instanceId;

		return Panache.withTransaction(entry::update);
	}

	public Uni<Long> delete(Long instanceId, Long id) {
		return Panache.withTransaction(() -> Entry.delete(instanceId, id));
	}

	public Uni<Long> delete(Long instanceId, List<Long> ids) {
		return Panache.withTransaction(() -> Entry.delete(instanceId, ids));
	}

}
