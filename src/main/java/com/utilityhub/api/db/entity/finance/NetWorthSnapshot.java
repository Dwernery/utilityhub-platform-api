package com.utilityhub.api.db.entity.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "net_worth_snapshots", uniqueConstraints = @UniqueConstraint(columnNames = "balance_month", name = "uq_net_worth_snapshot_date"))
public class NetWorthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "balance_month", nullable = false)
    private LocalDate balanceMonth;

    @Column(name = "net_worth", nullable = false, precision = 15, scale = 2)
    private BigDecimal netWorth;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getBalanceMonth() {
        return this.balanceMonth;
    }

    public void setBalanceMonth(LocalDate balanceMonth) {
        this.balanceMonth = balanceMonth;
    }

    public BigDecimal getNetWorth() {
        return this.netWorth;
    }

    public void setNetWorth(BigDecimal netWorth) {
        this.netWorth = netWorth;
    }

}
