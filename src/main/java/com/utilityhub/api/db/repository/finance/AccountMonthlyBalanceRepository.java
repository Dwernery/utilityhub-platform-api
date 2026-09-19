package com.utilityhub.api.db.repository.finance;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.utilityhub.api.db.entity.finance.AccountMonthlyBalance;

@Repository
public interface AccountMonthlyBalanceRepository extends JpaRepository<AccountMonthlyBalance, Integer> {
    @Query("""
            FROM AccountMonthlyBalance amb
            ORDER BY amb.balanceDate ASC, amb.account.name ASC
            """)
    List<AccountMonthlyBalance> findAllAccountBalancesWithDetails();
}
