package com.utilityhub.api.db.repository.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utilityhub.api.db.entity.finance.IncomeSource;

@Repository
public interface IncomeSourceRepository extends JpaRepository<IncomeSource, Integer> {}
