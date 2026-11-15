package com.utilityhub.api.db.repository.finance;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utilityhub.api.db.entity.finance.AccountType;

@Repository
public interface AccountTypeRepository extends JpaRepository<AccountType, Integer> {
    /** Account type names are unique in the database (account_types_name_key). */
    Optional<AccountType> findByName(String name);
}
