package net.durmont.piggybank;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import net.durmont.piggybank.service.TransactionService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class RecurringTransactionScheduler {

    @Inject
    TransactionService transactionService;

    @Scheduled(cron = "${piggy.recurring-transaction-scheduler.cron : 0 5 4 * * ?}")
    public void schedule() {
        transactionService.generateRecurring()
                .subscribe().with(o -> {
                    Log.info("Fin de génération des transactions récurrentes : " + o);
                }); // nothing specific to do with the final result...
    }

}
