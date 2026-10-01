package com.utilityhub.api.db.repository.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import com.utilityhub.api.db.entity.finance.AccountType;
import com.utilityhub.api.db.entity.finance.AccountCategory;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountTypeRepository extends JpaRepository<AccountType, Integer> {
    Optional<AccountType> findByNameAndCategory(String name, AccountCategory category);
}
