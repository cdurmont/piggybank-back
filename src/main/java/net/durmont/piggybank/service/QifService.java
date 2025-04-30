package net.durmont.piggybank.service;

import com.github.squirrelgrip.qif4j.QifCashTransaction;
import com.github.squirrelgrip.qif4j.QifReader;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.Account;
import net.durmont.piggybank.model.Entry;
import net.durmont.piggybank.model.Transaction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class QifService {
    
    @Inject TransactionService transactionService;
    
    public Uni<List<Transaction>> loadQif(Long instanceId, Long accountId, File qif) {
        List<Transaction> txns = new ArrayList<>();
        try (QifReader reader = new QifReader(qif,"dd/MM/yyyy") ) {
            reader.addTranasctionListener(qt -> {
                if (qt instanceof QifCashTransaction) {
                    QifCashTransaction qifTransaction = (QifCashTransaction) qt;
                    Log.info("Transaction : " + qifTransaction.getDate()+" "+ qifTransaction.getPayee()+" "+qifTransaction.getTotal());
                    txns.add(buildTransaction(accountId, qifTransaction));
                }
            });
            reader.load();  // txns now populated with Transactions to import

            return transactionService.createMany(instanceId, txns);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    
    private Transaction buildTransaction(Long accountId, QifCashTransaction qifTransaction) {
        Transaction txn = new Transaction();
        txn.type = "I";
        txn.description = qifTransaction.getPayee();
        Entry entry = new Entry();
        txn.entries = new ArrayList<>();
        txn.entries.add(entry);
        entry.transaction = txn;
        entry.reference = qifTransaction.getMemo();
        entry.account = new Account();
        entry.account.id = accountId;
        entry.setDateFromLocalDate(qifTransaction.getDate());
        if (qifTransaction.getTotal().doubleValue() < 0) {
            entry.credit = qifTransaction.getTotal().negate();
        }
        else {
            entry.debit = qifTransaction.getTotal();
        }

        return txn;
    }
}
