package com.utilityhub.api.db.repository.finance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utilityhub.api.db.entity.finance.NetWorthSnapshot;

@Repository
public interface NetWorthSnapshotRepository extends JpaRepository<NetWorthSnapshot, Integer> {

    /** Derived query: Spring Data builds "ORDER BY balanceMonth ASC" from the method name. */
    List<NetWorthSnapshot> findAllByOrderByBalanceMonthAsc();
}
