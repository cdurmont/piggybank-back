package net.durmont.piggybank.model;

import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import org.apache.commons.beanutils.BeanUtils;

import jakarta.persistence.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Entity
public class Entry extends ConvertedEntity implements Cloneable {

	@ManyToOne(targetEntity = Instance.class)
	public Instance instance;

	@Temporal(TemporalType.TIMESTAMP)
	public Date date;
	@ManyToOne(targetEntity = Account.class)
	public Account account;
	@ManyToOne(targetEntity = Transaction.class)
	public Transaction transaction;
	@Column(precision = 12, scale = 2)
	public BigDecimal debit;
	public BigDecimal credit;
	public String reference;
	public String description;
	public String mongoId;
	@Transient
	public BigDecimal balance;

	public Entry() {
	}

	public Entry(String json) {
		super(json);
	}

	/**
	 * Acts as a custom clone
	 *
	 * @param e Entry to copy
	 */
	public Entry(Entry e) {
		instance = e.instance;
		date = e.date;
		account = e.account;
		transaction = e.transaction;
		debit = e.debit;
		credit = e.credit;
		reference = e.reference;
		description = e.description;
	}

	public static Uni<List<Entry>> list(Long instanceId, Entry filter, Sort sort, Page page) {
		Map<String, Object> params = new HashMap<>();
		String query = "1=1";
		query += " AND instance.id=:instance_id";
		params.put("instance_id", instanceId);

		return Entry.find(query, sort, params)
			.page(page)
			.list();
	}

	public static Uni<List<Entry>> findByAccount(Long instanceId, Long accountId, boolean showReconciled) {
		Map<String, Object> params = new HashMap<>();
		params.put("instanceId", instanceId);
		params.put("accountId", accountId);
		params.put("showReconciled", showReconciled);

		return Entry.<Entry>find(
			"select distinct e from Entry e "
				+ "join fetch e.transaction t " +
				"join fetch t.entries " +
				"where e.account.id = :accountId " +
				"and t.instance.id = :instanceId " +
				"and t.type = 'S' " +
				"and (t.reconciled = false or :showReconciled = true) " +
				"order by e.date DESC "
			, params).list();
	}

	public static Uni<Long> delete(Long instanceId, Long id) {
		return Entry.delete("instance.id=:instance_id and id=:id", Parameters.with("instance_id", instanceId).and("id", id));
	}

	public static <T> Uni<Long> delete(Long instanceId, List<Long> ids) {

		return Entry.delete("instance.id=:instance_id and id in :ids", Parameters.with("instance_id", instanceId).and("ids", ids));
	}

	public void setDateFromLocalDate(LocalDate localDate) {
		date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	@Override
	public int hashCode() {
		int result = date != null ? date.hashCode() : 0;
		result = 31 * result + (account != null ? account.hashCode() : 0);
		result = 31 * result + (transaction != null ? transaction.hashCode() : 0);
		result = 31 * result + (debit != null ? debit.hashCode() : 0);
		result = 31 * result + (credit != null ? credit.hashCode() : 0);
		result = 31 * result + (reference != null ? reference.hashCode() : 0);
		result = 31 * result + (description != null ? description.hashCode() : 0);
		result = 31 * result + (mongoId != null ? mongoId.hashCode() : 0);
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Entry)) return false;

		Entry entry = (Entry) o;

		if (!Objects.equals(date, entry.date)) return false;
		if (!Objects.equals(account, entry.account)) return false;
		if (!Objects.equals(transaction, entry.transaction)) return false;
		if (!Objects.equals(debit, entry.debit)) return false;
		if (!Objects.equals(credit, entry.credit)) return false;
		if (!Objects.equals(reference, entry.reference)) return false;
		if (!Objects.equals(description, entry.description)) return false;
		return Objects.equals(mongoId, entry.mongoId);
	}

	@Override
	protected Entry clone() throws CloneNotSupportedException {
		Entry clone = (Entry) super.clone();
		clone.id = null;
		clone.date = new Date();
		return clone;
	}

	@Override
	public void setDefaults() {

	}

	public BigDecimal balance(BigDecimal previousValue) {
		balance = previousValue != null ? previousValue : BigDecimal.ZERO;
		if (debit != null)
			balance = balance.add(debit);
		if (credit != null)
			balance = balance.subtract(credit);

		return balance;
	}

	public Uni<Entry> update() {

		return Entry.<Entry>findById(id)
			.map(entryDb -> {
				if (entryDb == null || (entryDb.instance != null && !Objects.equals(instance.id, entryDb.instance.id)))
					return null;
				instance = entryDb.instance;
				try {
					BeanUtils.copyProperties(entryDb, this);
				} catch (IllegalAccessException | InvocationTargetException e) {
					Log.error("Error copying new properties of Entry " + id, e);
				}
				return entryDb;
			});

	}
}
