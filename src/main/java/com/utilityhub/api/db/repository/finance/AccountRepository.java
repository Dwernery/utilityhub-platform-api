package com.utilityhub.api.db.repository.finance;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utilityhub.api.db.entity.finance.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    /**
     * Same as the inherited findAll(), but an entity graph tells Hibernate to
     * join the account type into the query. This is the annotation-based
     * alternative to writing JOIN FETCH by hand.
     */
    @Override
    @EntityGraph(attributePaths = "accountType")
    List<Account> findAll();
}
