package com.utilityhub.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.utilityhub.api.db.entity.finance.Account;
import com.utilityhub.api.db.entity.finance.AccountCategory;
import com.utilityhub.api.db.entity.finance.AccountMonthlyBalance;
import com.utilityhub.api.db.entity.finance.AccountType;
import com.utilityhub.api.db.repository.finance.AccountMonthlyBalanceRepository;
import com.utilityhub.api.db.repository.finance.AccountRepository;
import com.utilityhub.api.db.repository.finance.AccountTypeRepository;
import com.utilityhub.api.db.repository.finance.NetWorthSnapshotRepository;
import com.utilityhub.api.dto.request.AccountCreateRequestDTO;
import com.utilityhub.api.dto.request.DeleteAccountBalanceRequestDTO;
import com.utilityhub.api.dto.request.EditAccountBalanceRequestDTO;
import com.utilityhub.api.dto.request.EditTransactionRequestDTO;
import com.utilityhub.api.dto.response.finance.AccountBalanceDTO;
import com.utilityhub.api.dto.response.finance.AccountResponseDTO;
import com.utilityhub.api.dto.response.finance.NetWorthHistoryResponseDTO;
import com.utilityhub.api.dto.response.finance.TransactionResponseDTO;
import com.utilityhub.api.db.repository.finance.TransactionRepository;

@Service
public class FinanceService {
        private static final Logger logger = LoggerFactory.getLogger(FinanceService.class);
        private final NetWorthSnapshotRepository netWorthSnapshotRepository;
        private final AccountMonthlyBalanceRepository accountMonthlyBalanceRepository;
        private final TransactionRepository transactionRepository;
        private final AccountRepository accountRepository;
        private final AccountTypeRepository accountTypeRepository;

        public FinanceService(NetWorthSnapshotRepository netWorthSnapshotRepository,
                        AccountMonthlyBalanceRepository accountMonthlyBalanceRepository,
                        TransactionRepository transactionRepository,
                        AccountRepository accountRepository,
                        AccountTypeRepository accountTypeRepository) {
                this.netWorthSnapshotRepository = netWorthSnapshotRepository;
                this.accountMonthlyBalanceRepository = accountMonthlyBalanceRepository;
                this.transactionRepository = transactionRepository;
                this.accountRepository = accountRepository;
                this.accountTypeRepository = accountTypeRepository;
        }

        public List<NetWorthHistoryResponseDTO> getCompleteNetWorthHistory() {
                // Get historical net worth snapshots
                List<NetWorthHistoryResponseDTO> historicalNetWorth = netWorthSnapshotRepository
                                .findAllNetWorthSnapshots()
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
                                                                                amb.getAccount().getId(),
                                                                                amb.getAccount().getName(),
                                                                                amb.getAccount().getAccountType()
                                                                                                .getName(),
                                                                                amb.getAccount().getAccountType()
                                                                                                .getCategory()
                                                                                                .toString(),
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
                                        return new NetWorthHistoryResponseDTO(date, "account-based", netWorth,
                                                        accounts);
                                })
                                .toList();

                return Stream.concat(historicalNetWorth.stream(), accountBalances.stream())
                                .sorted((a, b) -> {
                                        int dateCompare = a.getDate().compareTo(b.getDate());
                                        if (dateCompare != 0) {
                                                return dateCompare;
                                        }
                                        // Historical first, then account-based
                                        if (a.getSource().equals("historical")
                                                        && b.getSource().equals("account-based")) {
                                                return -1;
                                        }
                                        return 1;
                                })
                                .toList();
        }

        public void updateAccountBalance(EditAccountBalanceRequestDTO request) {
                logger.info("Attempting to find or create account balance: accountId={}, balanceDate={}",
                                request.accountId(), request.balanceDate());

                AccountMonthlyBalance accountMonthlyBalance = accountMonthlyBalanceRepository
                                .findByAccountIdAndBalanceDate(request.accountId(), request.balanceDate())
                                .orElseGet(() -> {
                                        logger.info("Account balance not found, creating new record for accountId={}, balanceDate={}",
                                                        request.accountId(), request.balanceDate());
                                        Account account = accountRepository.findById(request.accountId())
                                                        .orElseThrow(() -> {
                                                                String errorMsg = "Account not found for account id: "
                                                                                + request.accountId();
                                                                logger.error(errorMsg);
                                                                return new RuntimeException(errorMsg);
                                                        });
                                        AccountMonthlyBalance newBalance = new AccountMonthlyBalance();
                                        newBalance.setAccount(account);
                                        newBalance.setBalanceDate(request.balanceDate());
                                        return newBalance;
                                });

                logger.info("Setting balance from {} to {} for accountId={}",
                                accountMonthlyBalance.getBalance(), request.balance(), request.accountId());

                accountMonthlyBalance.setBalance(request.balance());
                accountMonthlyBalanceRepository.save(accountMonthlyBalance);

                logger.info("Account balance saved successfully for accountId={}", request.accountId());
        }

        public List<TransactionResponseDTO> getAllTransactions() {
                return transactionRepository.findAll()
                                .stream()
                                .map(transaction -> new TransactionResponseDTO(
                                                transaction.getId(),
                                                transaction.getName(),
                                                transaction.getTransactionType(),
                                                transaction.getAmount(),
                                                transaction.isPaid(),
                                                transaction.isCash()))
                                .toList();
        }

        public List<AccountResponseDTO> getAllAccounts() {
                return accountRepository.findAll()
                                .stream()
                                .map(account -> new AccountResponseDTO(
                                                account.getId(),
                                                account.getName(),
                                                account.getAccountType().getName(),
                                                account.getAccountType().getCategory().toString()))
                                .toList();
        }

        public AccountResponseDTO createAccount(AccountCreateRequestDTO request) {
                logger.info("Creating new account: accountName={}, accountType={}, category={}",
                                request.accountName(), request.accountType(), request.category());

                try {
                        AccountCategory category = AccountCategory.valueOf(request.category().toUpperCase());

                        AccountType accountType = accountTypeRepository
                                        .findByNameAndCategory(request.accountType(), category)
                                        .orElseGet(() -> {
                                                logger.info("AccountType not found, creating new: name={}, category={}",
                                                                request.accountType(), category);
                                                AccountType newAccountType = new AccountType();
                                                newAccountType.setName(request.accountType());
                                                newAccountType.setCategory(category);
                                                return accountTypeRepository.save(newAccountType);
                                        });

                        Account account = new Account();
                        account.setName(request.accountName());
                        account.setAccountType(accountType);
                        Account savedAccount = accountRepository.save(account);

                        logger.info("Account created successfully: id={}, name={}", savedAccount.getId(),
                                        savedAccount.getName());

                        // Create initial monthly balance if provided
                        if (request.balance() != null && request.balanceDate() != null) {
                                logger.info("Creating initial balance for account: id={}, balance={}, balanceDate={}",
                                                savedAccount.getId(), request.balance(), request.balanceDate());
                                AccountMonthlyBalance monthlyBalance = new AccountMonthlyBalance();
                                monthlyBalance.setAccount(savedAccount);
                                monthlyBalance.setBalance(request.balance());
                                monthlyBalance.setBalanceDate(request.balanceDate());
                                accountMonthlyBalanceRepository.save(monthlyBalance);
                                logger.info("Initial balance created successfully for account: id={}",
                                                savedAccount.getId());
                        }

                        return new AccountResponseDTO(
                                        savedAccount.getId(),
                                        savedAccount.getName(),
                                        savedAccount.getAccountType().getName(),
                                        savedAccount.getAccountType().getCategory().toString());
                } catch (IllegalArgumentException e) {
                        String errorMsg = "Invalid category: " + request.category() + ". Must be ASSET or LIABILITY";
                        logger.error(errorMsg, e);
                        throw new RuntimeException(errorMsg, e);
                }
        }

        public void deleteAccountBalance(DeleteAccountBalanceRequestDTO request) {
                logger.info("Attempting to delete account balance: accountId={}, balanceDate={}",
                                request.accountId(), request.balanceDate());

                AccountMonthlyBalance accountMonthlyBalance = accountMonthlyBalanceRepository
                                .findByAccountIdAndBalanceDate(request.accountId(), request.balanceDate())
                                .orElseThrow(() -> {
                                        String errorMsg = "Account balance not found for account id: "
                                                        + request.accountId() + " and balance date: "
                                                        + request.balanceDate();
                                        logger.error(errorMsg);
                                        return new RuntimeException(errorMsg);
                                });

                logger.info("Deleting account balance record for accountId={}, balanceDate={}",
                                request.accountId(), request.balanceDate());
                accountMonthlyBalanceRepository.delete(accountMonthlyBalance);

                logger.info("Account balance deleted successfully for accountId={}", request.accountId());
        }

        public void editTransaction(EditTransactionRequestDTO request) {
                logger.info("Attempting to edit transaction: id={}, name={}, amount={}, paid={}",
                                request.id(), request.name(), request.amount(), request.paid());

                com.utilityhub.api.db.entity.finance.Transaction transaction = transactionRepository
                                .findById(request.id())
                                .orElseThrow(() -> {
                                        String errorMsg = "Transaction not found for id: " + request.id();
                                        logger.error(errorMsg);
                                        return new RuntimeException(errorMsg);
                                });

                transaction.setName(request.name());
                transaction.setAmount(request.amount());
                transaction.setPaid(request.paid());

                transactionRepository.save(transaction);
                logger.info("Transaction updated successfully: id={}", request.id());
        }

        public void clearAllTransactionsPaid() {
                logger.info("Clearing all transactions - setting paid to false");
                List<com.utilityhub.api.db.entity.finance.Transaction> allTransactions = transactionRepository
                                .findAll();

                allTransactions.forEach(transaction -> transaction.setPaid(false));
                transactionRepository.saveAll(allTransactions);

                logger.info("All transactions cleared successfully - {} transactions updated", allTransactions.size());
        }
}
