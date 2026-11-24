package com.utilityhub.api.db.repository.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.utilityhub.api.db.entity.finance.AccountMonthlyBalance;

@Repository
public interface AccountMonthlyBalanceRepository extends JpaRepository<AccountMonthlyBalance, Integer> {
    /**
     * JOIN FETCH loads each balance's account and account type in the same
     * query. Without it, Hibernate runs one extra SELECT per account (N+1).
     */
    @Query("""
            SELECT amb FROM AccountMonthlyBalance amb
            JOIN FETCH amb.account a
            JOIN FETCH a.accountType
            ORDER BY amb.balanceDate ASC, a.name ASC
            """)
    List<AccountMonthlyBalance> findAllAccountBalancesWithDetails();

    @Query("""
            FROM AccountMonthlyBalance amb
            WHERE amb.account.id = :accountId AND amb.balanceDate = :balanceDate
            """)
    Optional<AccountMonthlyBalance> findByAccountIdAndBalanceDate(
            @Param("accountId") Integer accountId, @Param("balanceDate") LocalDate balanceDate);

    /**
     * Insert-or-update in a single atomic statement (PostgreSQL "upsert").
     * A find-then-insert in Java has a race: two concurrent requests can both
     * see "not found" and both INSERT, and the second violates the unique
     * constraint. @Transactional does not prevent that; ON CONFLICT does.
     */
    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO account_monthly_balances (account_id, balance_date, balance)
            VALUES (:accountId, :balanceDate, :balance)
            ON CONFLICT (account_id, balance_date) DO UPDATE SET balance = EXCLUDED.balance
            """)
    void upsertBalance(
            @Param("accountId") Integer accountId,
            @Param("balanceDate") LocalDate balanceDate,
            @Param("balance") BigDecimal balance);

    /** Whether any account that isn't archived has a balance on this date (something a copy would bring forward). */
    @Query("""
            SELECT count(amb) > 0 FROM AccountMonthlyBalance amb
            WHERE amb.balanceDate = :balanceDate AND amb.account.archived = false
            """)
    boolean existsActiveBalanceOn(@Param("balanceDate") LocalDate balanceDate);

    /**
     * Copies every balance dated {@code fromDate} to {@code toDate} in one
     * statement, so it all happens or none of it does. ON CONFLICT DO NOTHING
     * skips accounts that already have a {@code toDate} balance: a copy never
     * overwrites what was entered by hand. Archived accounts aren't carried
     * forward. Returns the number of rows created.
     */
    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO account_monthly_balances (account_id, balance_date, balance)
            SELECT b.account_id, :toDate, b.balance
            FROM account_monthly_balances b
            JOIN accounts a ON a.id = b.account_id
            WHERE b.balance_date = :fromDate AND NOT a.archived
            ON CONFLICT (account_id, balance_date) DO NOTHING
            """)
    int copyBalances(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
