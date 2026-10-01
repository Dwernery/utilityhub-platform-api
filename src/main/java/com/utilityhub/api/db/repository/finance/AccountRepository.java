package com.utilityhub.api.db.repository.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import com.utilityhub.api.db.entity.finance.Account;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
}
