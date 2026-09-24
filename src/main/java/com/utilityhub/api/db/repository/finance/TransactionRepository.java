package com.utilityhub.api.db.repository.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import com.utilityhub.api.db.entity.finance.Transaction;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
}
