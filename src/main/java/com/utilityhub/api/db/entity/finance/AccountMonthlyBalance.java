package com.utilityhub.api.db.entity.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "account_monthly_balances",
        uniqueConstraints =
                @UniqueConstraint(
                        columnNames = {"account_id", "balance_date"},
                        name = "uq_account_monthly_balance"))
@Getter
@Setter
public class AccountMonthlyBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "balance_date", nullable = false)
    private LocalDate balanceDate;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;
}
