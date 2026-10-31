package com.utilityhub.api.db.repository.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import com.utilityhub.api.db.entity.finance.IncomeSource;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomeSourceRepository extends JpaRepository<IncomeSource, Integer> {
}
