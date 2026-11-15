package com.utilityhub.api.db.repository.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.utilityhub.api.db.entity.finance.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    /**
     * One UPDATE statement instead of loading every row, changing it in memory
     * and writing each one back. clearAutomatically discards any now-stale
     * entities from the persistence context.
     *
     * @return the number of rows changed
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Transaction t SET t.paid = false WHERE t.paid = true")
    int markAllUnpaid();
}
