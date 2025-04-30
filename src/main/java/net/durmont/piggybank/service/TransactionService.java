package net.durmont.piggybank.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.Entry;
import net.durmont.piggybank.model.Instance;
import net.durmont.piggybank.model.Transaction;
import org.apache.commons.beanutils.BeanUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class TransactionService {

    @Inject EntryService entryService;

    @ReactiveTransactional
    public Uni<Transaction> create(Long instanceId, Transaction newTxn) {
        newTxn.setDefaults();
        if (newTxn.instance == null)
            newTxn.instance = new Instance();
        newTxn.instance.id = instanceId;
        return newTxn.<Transaction>persist()    // 1. create new Transaction
                .chain(txnDb -> {                                       // 2. create entries and link them to the Transaction
                    List<Uni<Entry>> unis = new ArrayList<>();
                    if (txnDb != null && txnDb.entries != null)
                        unis.addAll(txnDb.entries.stream().map(entry -> {
                            entry.transaction = txnDb;
                            return entryService.create(instanceId, entry);
                        }).collect(Collectors.toList()));
                    return Uni.join().all(unis).andFailFast();
                })
                .map( unisResults -> unisResults.get(0).transaction);   // 3. get persisted Transaction from first Entry
    }

    @ReactiveTransactional
    public Uni<List<Transaction>> createMany(Long instanceId, List<Transaction> txns) {
        List<Uni<Transaction>> uniTxns = new ArrayList<>();
        for (Transaction txn : txns) {
            uniTxns.add(create(instanceId, txn));
        }
        return Uni.combine().all().unis(uniTxns).combinedWith(txnsDb -> (List<Transaction>) txnsDb);
    }


    public Uni<List<Transaction>> list(Long instanceId, Transaction filter, Sort sort, Page page) {
        Map<String, Object> params = new HashMap<>();
        String query ="select t from Transaction t join fetch t.entries where 1=1";
        query+=" AND t.instance.id=:instance_id";
        params.put("instance_id", instanceId);
        if (filter != null) {
            if (filter.balanced!= null) {
                query+=" AND t.balanced=:balanced";
                params.put("balanced", filter.balanced);
            }
            if (filter.reconciled!= null) {
                query+=" AND t.reconciled=:reconciled";
                params.put("reconciled", filter.reconciled);
            }
            if (filter.type != null) {
                query+=" AND t.type=:type";
                params.put("type", filter.type);
            }
            if (filter.owner != null && filter.owner.id != null) {
                query+=" AND t.owner.id=:owner_id";
                params.put("owner_id", filter.owner.id);
            }
            if (filter.txnBankId != null) {
                query+=" AND t.txnBankId = :txnBankId";
                params.put("txnBankId", filter.txnBankId);
            }

        }
        return Transaction.<Transaction>find(query, sort, params)
                .page(page)
                .list()
                .map(txns -> {
                    if (txns != null)
                        for (Transaction txn : txns) {
                            if (txn.entries != null)
                                for (Entry e : txn.entries) {
                                    e.transaction = null;   // break recursion in JSON rendering
                                }
                        }
                    return txns;
                });
    }

    public Uni<Transaction> findById(Long instanceId, Long id) {
        // will return the transaction UNLESS it is not from the given instance
        return Transaction.<Transaction>find("select t from Transaction t join fetch t.entries where t.id = ?1", id)
                .firstResult()
                .map(txn -> {
                    if (txn.entries != null)
                        for (Entry e : txn.entries) {
                            e.transaction = null;   // break recursion in JSON rendering
                        }
                    return txn != null && txn.instance != null && Objects.equals(instanceId, txn.instance.id) ? txn : null;
                });
    }

    /**
     * Updates the full transaction, including entries
     * Upon update, entries are deleted and recreated
     * @param instanceId target instance's id
     * @param id id of Transaction to update
     * @param txn Transaction object containing changes to be applied
     * @return updated Transaction, or null in case of an error, or if there's something wrong with instance ids
     */
    public Uni<Transaction> update(Long instanceId, Long id, Transaction txn) {
        txn.id=id;
        txn.setDefaults();
        return Panache.withTransaction(
                () -> findById(instanceId, id)  // 1.read full transaction w/ entries
                        .chain(txnDb -> {
                            // list of entry ids to delete. Better build this one before using copyProperties !
                            List<Long> ids = new ArrayList<>();
                            if (txnDb != null && txnDb.entries != null)
                                ids = txnDb.entries.stream().map(entry -> entry.id).collect(Collectors.toList());

                            // 2.update transaction (implicitly by updating an attached Entity)
                            try {
                                BeanUtils.copyProperties(txnDb, txn);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                Log.error("Error copying new properties of Transaction "+id, e);
                            }

                            return entryService.delete(instanceId, ids);    // 3. delete existing entries
                        })
                        .chain(() -> {
                            List<Uni<Entry>> uniEntries = new ArrayList<>();
                            if (txn.entries != null)
                                for( Entry entry : txn.entries) {
                                    entry.id = null;
                                    entry.transaction = txn;
                                    uniEntries.add(entryService.create(instanceId, entry));
                                }
                            return Uni.join().all(uniEntries).andFailFast();    // 4.create entries
                        })
                        .map( createResult -> {
                            txn.entries = null;
                            return txn;
                        })    // the last mapping is somewhat bogus, if we made it to this point without failing, we assume everything is ok, and txn is returned (txnDb would be more accurate...)
        );
    }


    public Uni<Long> delete(Long instanceId, Long id) {
        return Panache.withTransaction(
                () -> findById(instanceId, id)  // 1.read full transaction w/ entries
                        .chain(txnDb -> {
                            List<Long> ids = new ArrayList<>();
                            if (txnDb != null && txnDb.entries != null)
                                ids = txnDb.entries.stream().map(entry -> entry.id).collect(Collectors.toList());   // list of entry ids to delete
                            return entryService.delete(instanceId, ids);    // 2.delete entries
                        })
                        .chain(() -> Transaction.delete("instance.id=:instance_id and id=:id", Parameters.with("instance_id",instanceId).and("id",id))) // 3.delete transaction
                );
    }

    public Uni<List<Transaction>> listPendingRecurring() {
        Map<String, Object> params = new HashMap<>();
        String query ="select t from Transaction t join fetch t.entries where 1=1";
        query+=" AND t.type = :type";
        params.put("type", "R");
        query+=" AND t.recurStartDate < :now";
        params.put("now", LocalDate.now());
        query+=" AND (t.recurEndDate is null or t.recurEndDate > :now)";
        query+=" AND t.recurNextDate is not null";
        query+=" AND t.recurNextDate < :now";

        return Transaction.<Transaction>find(query, params)
                .list()
                .map(txns -> new ArrayList<>(new HashSet<>(txns))); // quick deduplication, as the query strangely returns as many references to a transaction than the number of entries within
    }

    public Uni<List<Transaction>> generateRecurring() {
        return         Panache.withTransaction(() ->
                listPendingRecurring()
                        .chain(transactions -> {
                            List<Uni<Transaction>> uniTxns = new ArrayList<>();
                            if (transactions != null) {
                                for (Transaction txnRecur : transactions) {
                                    Transaction txnInstance = createInstance(txnRecur);
                                    txnRecur.recurNextDate = getNextDate(txnRecur); // FIXME Quarkus bug ???
                                    uniTxns.add(create(txnInstance.instance.id, txnInstance));
                                }
                                return Uni.join().all(uniTxns).andFailFast();    // 2. persist new standard txns
                            }
                            else
                                return Uni.createFrom().nullItem();
                        })
//                        .chain(transactions -> listPendingRecurring())
//                        .chain(transactions -> {
//                            List<Uni<Transaction>> uniTxns = new ArrayList<>();
//                            if (transactions != null) {
//                                for (Transaction txnRecur : transactions) {
//                                    Transaction txnModif = new Transaction(txnRecur);
//                                    txnModif.recurNextDate = getNextDate(txnModif);  // update recurring txns with new recurNextDate
//                                    uniTxns.add(update(txnRecur.instance.id, txnRecur.id, txnModif));
//                                }
//                            }
//                            return Uni.join().all(uniTxns).andFailFast();
//                        })
        );
    }
    public Uni<List<Transaction>> updateRecurringNextDate() {
        return         Panache.withTransaction(() ->
                listPendingRecurring()
                        .map(transactions -> {
                            if (transactions != null) {
                                for (Transaction txnRecur : transactions) {
                                    txnRecur.recurNextDate = getNextDate(txnRecur);  // update recurring txns with new recurNextDate
                                }
                            }
                            return transactions;
                        })
        );
    }

    private LocalDate getNextDate(Transaction txnRecur) {
        if (txnRecur.recurNextDate != null) {
            LocalDate newRecurNextDate = txnRecur.recurNextDate.plusMonths(1);
            if (txnRecur.recurEndDate == null || newRecurNextDate.isBefore(txnRecur.recurEndDate))  // apply new date if consistent with recurEndDate
                return newRecurNextDate;

        }
        return null;
    }

    private Transaction createInstance(Transaction txnRecur) {
        Transaction txnInstance = new Transaction(txnRecur);
        LocalDate instanceDate = txnRecur.recurNextDate;
        if (txnInstance.entries != null)
            for (Entry e : txnInstance.entries)
                e.setDateFromLocalDate(instanceDate);

        txnInstance.type = "S";
        txnInstance.recurStartDate = null;
        txnInstance.recurNextDate = null;
        txnInstance.recurEndDate = null;
        if (txnInstance.description == null)
            txnInstance.description = "";
        txnInstance.description += " (" + instanceDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))  + ")";

        return txnInstance;
    }

}
