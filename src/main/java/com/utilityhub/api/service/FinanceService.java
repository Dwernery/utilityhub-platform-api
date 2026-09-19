package com.utilityhub.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.utilityhub.api.db.repository.finance.AccountMonthlyBalanceRepository;
import com.utilityhub.api.db.repository.finance.NetWorthSnapshotRepository;
import com.utilityhub.api.dto.response.finance.AccountBalanceDTO;
import com.utilityhub.api.dto.response.finance.NetWorthHistoryResponseDTO;

@Service
public class FinanceService {
    private final NetWorthSnapshotRepository netWorthSnapshotRepository;
    private final AccountMonthlyBalanceRepository accountMonthlyBalanceRepository;

    public FinanceService(NetWorthSnapshotRepository netWorthSnapshotRepository, AccountMonthlyBalanceRepository accountMonthlyBalanceRepository) {
        this.netWorthSnapshotRepository = netWorthSnapshotRepository;
        this.accountMonthlyBalanceRepository = accountMonthlyBalanceRepository;
    }

    public List<NetWorthHistoryResponseDTO> getCompleteNetWorthHistory() {
        // Get historical net worth snapshots
        List<NetWorthHistoryResponseDTO> historicalNetWorth = netWorthSnapshotRepository.findAllNetWorthSnapshots()
                .stream()
                .map(snapshot -> new NetWorthHistoryResponseDTO(
                        snapshot.getBalanceMonth(),
                        "historical",
                        snapshot.getNetWorth(),
                        null))
                .toList();

        // Get individual account balances and group by date
        Map<LocalDate, List<AccountBalanceDTO>> accountsByDate = accountMonthlyBalanceRepository
                .findAllAccountBalancesWithDetails()
                .stream()
                .collect(Collectors.groupingBy(
                        amb -> amb.getBalanceDate(),
                        Collectors.mapping(
                                amb -> new AccountBalanceDTO(
                                        amb.getAccount().getName(),
                                        amb.getAccount().getAccountType().getName(),
                                        amb.getAccount().getAccountType().getCategory().toString(),
                                        amb.getBalance()),
                                Collectors.toList())));

        List<NetWorthHistoryResponseDTO> accountBalances = accountsByDate.entrySet()
                .stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<AccountBalanceDTO> accounts = entry.getValue();

                    java.math.BigDecimal assets = accounts.stream()
                            .filter(acc -> "ASSET".equals(acc.getCategory()))
                            .map(AccountBalanceDTO::getBalance)
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                    java.math.BigDecimal liabilities = accounts.stream()
                            .filter(acc -> "LIABILITY".equals(acc.getCategory()))
                            .map(AccountBalanceDTO::getBalance)
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                    java.math.BigDecimal netWorth = assets.subtract(liabilities);
                    return new NetWorthHistoryResponseDTO(date, "account-based", netWorth, accounts);
                })
                .toList();

        return Stream.concat(historicalNetWorth.stream(), accountBalances.stream())
                .sorted((a, b) -> {
                    int dateCompare = a.getDate().compareTo(b.getDate());
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    // Historical first, then account-based
                    if (a.getSource().equals("historical") && b.getSource().equals("account-based")) {
                        return -1;
                    }
                    return 1;
                })
                .toList();
    }
}
