package net.durmont.piggybank.model;

import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import org.apache.commons.beanutils.BeanUtils;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@NamedQueries({
	@NamedQuery(name = "balance", query = "select coalesce(sum(e.debit),0)-coalesce(sum(e.credit),0) " +
		"from Entry e join e.transaction t " +
		"where e.account.id IN :accountIds " +
		"and t.type = 'S'"),
	@NamedQuery(name = "stats", query = "select new net.durmont.piggybank.model.Stat(year(e.date), month(e.date), coalesce(sum(e.debit),0),coalesce(sum(e.credit),0)) " +
		"from Entry e join e.transaction t " +
		"where e.account.id IN :accountIds " +
		"and t.type = 'S' " +
		"group by year(e.date), month(e.date) " +
		"order by year(e.date), month(e.date) "),

})
public class Account extends ConvertedEntity implements Cloneable, Comparable<Account> {

	@ManyToOne(targetEntity = Instance.class)
	public Instance instance;
	@Column(length = 1)
	public String type;
	public String name;
	public String externalRef;
	// stores the Piggybank-Link linked account reference
	public String linkId;
	@ManyToOne(targetEntity = Account.class)
	public Account parent;
	public Boolean colorRevert;
	public Boolean reconcilable;
	public String mongoId;
	public String color;
	public String icon;

	@Transient
	public Boolean root;
	@Transient
	public Collection<Account> subAccounts;

	public Account() {
	}

	public Account(String json) {
		super(json);
	}

	public static Uni<BigDecimal> getBalance(Long instanceId, @NotNull Long id) {
		return Account.getSession()
			.flatMap(
				session -> getChildrenAccountIds(instanceId, id).onItem().transformToUni(
					ids -> {
						ids.add(id);    // add main account's id too
						return session.<BigDecimal>createNamedQuery("balance").setParameter("accountIds", ids).getSingleResult();
					}));
	}

	public static Uni<List<Stat>> getStats(Long instanceId, @NotNull Long id) {
		return Account.getSession()
			.chain(
				session -> getChildrenAccountIds(instanceId, id).onItem().transformToUni(
					ids -> {
						ids.add(id);    // add main account's id too
						return session.createNamedQuery("stats", Stat.class).setParameter("accountIds", ids).getResultList();
					}))
			;
	}

	public static Uni<List<Account>> listLinked(Long instanceId, Sort sort, Page page) {
		Map<String, Object> params = new HashMap<>();
		String query = "1=1";
		query += " AND instance.id=:instance_id";
		params.put("instance_id", instanceId);
		query += " AND linkId IS NOT NULL";

		return Account.find(query, sort, params)
			.page(page)
			.list();
	}

	public static Uni<Long> delete(Long instanceId, Long id) {
		return Account.delete("instance.id=:instance_id and id=:id", Parameters.with("instance_id", instanceId).and("id", id));
	}

	public static Uni<List<Account>> list(Long instanceId, Account filter, Sort sort, Page page) {

		Map<String, Object> params = new HashMap<>();
		String query = "1=1";
		query += " AND instance.id=:instance_id";
		params.put("instance_id", instanceId);
		if (filter != null) {
			if (filter.id != null) {
				query += " AND id=:id";
				params.put("id", filter.id);
			}
			if (filter.colorRevert != null) {
				query += " AND colorRevert=:colorRevert";
				params.put("colorRevert", filter.colorRevert);
			}
			if (filter.reconcilable != null) {
				query += " AND reconcilable=:reconcilable";
				params.put("reconcilable", filter.reconcilable);
			}
			if (filter.externalRef != null) {
				query += " AND externalRef=:externalRef";
				params.put("externalRef", filter.externalRef);
			}
			if (filter.mongoId != null) {
				query += " AND mongoId=:mongoId";
				params.put("mongoId", filter.mongoId);
			}
			if (filter.name != null) {
				query += " AND name=:name";
				params.put("name", filter.name);
			}
			if (filter.type != null) {
				query += " AND type=:type";
				params.put("type", filter.type);
			}
			if (filter.parent != null && filter.parent.id != null) {
				query += " AND parent.id=:parent_id";
				params.put("parent_id", filter.parent.id);
			} else if (filter.root == Boolean.TRUE) {
				query += " AND parent is null";
			}
		}
		return Account.find(query, sort, params)
			.page(page)
			.list();
	}

	public static Uni<List<Long>> getChildrenAccountIds(Long instanceId, Long accountId) {
		return Account.getAllAccounts(instanceId).map(accounts -> getChildrenAccountIdsInternal(accounts, accountId));
	}

	private static List<Long> getChildrenAccountIdsInternal(List<Account> accounts, Long accountId) {
		List<Long> ids = new ArrayList<>();
		// step 1 : get the Account
		Account acc = accounts.stream().filter(account -> Objects.equals(account.id, accountId))
			.findAny().orElse(null);
		// step 2 : add the subAccounts to the list
		if (acc != null) {
			if (acc.subAccounts != null)
				for (Account sub : acc.subAccounts) {
					ids.add(sub.id);
					ids.addAll(getChildrenAccountIdsInternal(accounts, sub.id));
				}
		}
		return ids;
	}

	public static Uni<List<Account>> getAllAccounts(Long instanceId) {

		return Account.<Account>list("instance.id", instanceId)
			.onItem().ifNotNull()
			.transform(Account::relinkAccounts);
	}

	private static List<Account> relinkAccounts(List<Account> accounts) {
		// create a Map of accounts indexed on ids
		Map<Long, Account> accountMap = accounts.stream().collect(Collectors.toMap(Account::getId, Function.identity()));
		for (Account account : accounts) {
			if (account.parent != null) {   // if this account has a parent, find the parent through the Map...
				Account parent = accountMap.get(account.parent.id);
				if (parent.subAccounts == null)
					parent.subAccounts = new TreeSet<>();   // sorted automatically using Account::compareTo
				parent.subAccounts.add(account); // then add the account as a sub of the parent
			}
		}
		return accounts;
	}

	public void setDefaults() {
		if (colorRevert == null) colorRevert = false;
		if (reconcilable == null) reconcilable = false;
		if (color == null) color = "blue";
		if (icon == null) icon = "pi pi-tag";
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Account)) return false;

		Account account = (Account) o;

		return Objects.equals(id, account.id);
	}

	@Override
	public Account clone() {
		Account clone;
		try {
			clone = (Account) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		return clone;
	}

	public Long getId() {
		return id;
	}

	@Override
	public int compareTo(@NotNull Account o) {
		if (name == null && (o.name == null))
			return 0;
		if (name == null)   // l'autre nom est non-null (cf. au-dessus). Je mets les comptes sans nom en 1er
			return -1;
		// maintenant name != null
		if (o.name == null)
			return 1;
		return name.compareTo(o.name);
	}

	public Uni<Account> update() {
		return Account.<Account>findById(id)
			.map(accountDb -> {
				if (accountDb != null && accountDb.instance != null && !Objects.equals(instance.id, accountDb.instance.id))
					return null;
				if (accountDb != null) {
					instance = accountDb.instance;
					try {
						BeanUtils.copyProperties(accountDb, this);
					} catch (IllegalAccessException | InvocationTargetException e) {
						Log.error("Error copying new properties of Account " + id, e);
					}
				}
				return accountDb;
			});
	}
}
