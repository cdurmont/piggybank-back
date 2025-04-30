package net.durmont.piggybank.service;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.Account;
import net.durmont.piggybank.model.Entry;
import net.durmont.piggybank.model.Instance;
import net.durmont.piggybank.model.Stat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class AccountService {

	public Uni<List<Account>> getAccountTree(Long instanceId) {

		return Account.getAllAccounts(instanceId).map(
			accounts -> accounts.stream()                       // from the list of all Accounts for this instance
				.peek(account -> {
					if (account.root == null) {
						if (account.parent != null) {
							account.parent = null;                    // since we put a lot of energy on building the subAccounts lists, delete the parent instead
							account.root = Boolean.FALSE;
						} else
							account.root = Boolean.TRUE;
					}
				})
				.filter(account -> account.root == Boolean.TRUE)  // keep only those without parent (root Accounts)
				.sorted()
				.collect(Collectors.toList())
		);
	}

	public Uni<List<Account>> list(Long instanceId, Account filter, Sort sort, Page page) {
		return Account.list(instanceId, filter, sort, page);
	}



	public Uni<BigDecimal> balance(Long instanceId, @NotNull Long id) {
		return Account.getBalance(instanceId, id);
	}

	public Uni<List<Stat>> stats(Long instanceId, @NotNull Long id) {
		return Account.getStats(instanceId, id);
	}

	public Uni<Account> findById(Long instanceId, Long id) {
		// will return the account UNLESS it is not from the given instance
		return Account.<Account>findById(id)
			.map(account -> account != null && account.instance != null && Objects.equals(instanceId, account.instance.id) ? account : null);
	}

	public Uni<Account> create(Long instanceId, Account newAccount) {
		newAccount.setDefaults();
		if (newAccount.instance == null)
			newAccount.instance = new Instance();
		newAccount.instance.id = instanceId;
		return Panache.withTransaction(newAccount::persist);
	}

	public Uni<Account> update(Long instanceId, Long id, Account account) {
		account.id = id;
		account.setDefaults();
		if (account.instance == null)
			account.instance = new Instance();
		account.instance.id = instanceId;
		return Panache.withTransaction(account::update);
	}

	@CacheInvalidate(cacheName = "accounts-cache")
	public Uni<Long> delete(Long instanceId, Long id) {
		return Panache.withTransaction(() -> Account.delete(instanceId, id));
	}

	public Uni<List<Entry>> getEntries(Long instanceId, Long accountId, boolean showReconciled, Page page) {
		if (accountId <= 0)
			return null;
		return Entry.findByAccount(instanceId, accountId, showReconciled)
			.map(entries -> {
				// remove unneeded stuff
				for (Entry entry : entries) {
					entry.transaction.entries.remove(entry);    // remove main entry from "contreparties"
					for (Entry ctp : entry.transaction.entries)
						ctp.transaction = null; // stop infinite recursion through lazy loading
				}
				// compute balance
				BigDecimal balance = BigDecimal.ZERO;
				for (int i = entries.size() - 1; i >= 0; i--) {
					balance = entries.get(i).balance(balance);
				}
				// pagination
				int startIndex = page.size * page.index;
				int endIndex = page.size * (page.index + 1);
				if (endIndex > entries.size())
					endIndex = entries.size();
				return entries.subList(startIndex, endIndex);
			});
	}

	public Uni<List<Account>> listLinked(Long instanceId, Sort sort, Page page) {
		return Account.listLinked(instanceId, sort, page);
	}
}
