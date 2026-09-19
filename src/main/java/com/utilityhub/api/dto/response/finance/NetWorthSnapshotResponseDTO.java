package com.utilityhub.api.dto.response.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NetWorthSnapshotResponseDTO {
    private LocalDate balanceMonth;
    private BigDecimal netWorth;

    public NetWorthSnapshotResponseDTO(LocalDate balanceMonth, BigDecimal netWorth) {
        this.balanceMonth = balanceMonth;
        this.netWorth = netWorth;
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
