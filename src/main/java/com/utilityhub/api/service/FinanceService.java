package com.utilityhub.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utilityhub.api.db.entity.finance.Account;
import com.utilityhub.api.db.entity.finance.AccountCategory;
import com.utilityhub.api.db.entity.finance.AccountMonthlyBalance;
import com.utilityhub.api.db.entity.finance.AccountType;
import com.utilityhub.api.db.entity.finance.IncomeSource;
import com.utilityhub.api.db.entity.finance.Transaction;
import com.utilityhub.api.db.repository.finance.AccountMonthlyBalanceRepository;
import com.utilityhub.api.db.repository.finance.AccountRepository;
import com.utilityhub.api.db.repository.finance.AccountTypeRepository;
import com.utilityhub.api.db.repository.finance.IncomeSourceRepository;
import com.utilityhub.api.db.repository.finance.NetWorthSnapshotRepository;
import com.utilityhub.api.db.repository.finance.TransactionRepository;
import com.utilityhub.api.dto.request.AccountCreateRequestDTO;
import com.utilityhub.api.dto.request.AccountUpdateRequestDTO;
import com.utilityhub.api.dto.request.IncomeSourceCreateRequestDTO;
import com.utilityhub.api.dto.request.IncomeSourceUpdateRequestDTO;
import com.utilityhub.api.dto.request.TransactionCreateRequestDTO;
import com.utilityhub.api.dto.request.TransactionUpdateRequestDTO;
import com.utilityhub.api.dto.response.finance.AccountBalanceResponseDTO;
import com.utilityhub.api.dto.response.finance.AccountResponseDTO;
import com.utilityhub.api.dto.response.finance.BalanceCopyResponseDTO;
import com.utilityhub.api.dto.response.finance.IncomeSourceResponseDTO;
import com.utilityhub.api.dto.response.finance.NetWorthHistoryResponseDTO;
import com.utilityhub.api.dto.response.finance.TransactionResponseDTO;
import com.utilityhub.api.exception.InvalidRequestException;
import com.utilityhub.api.exception.ResourceNotFoundException;

/**
 * Class-level {@code @Transactional(readOnly = true)} makes every method run in
 * a read-only transaction by default; methods that write override it with a
 * plain {@code @Transactional}. If a write method throws a RuntimeException,
 * every statement it ran is rolled back together.
 */
@Service
@Transactional(readOnly = true)
public class FinanceService {
    private static final Logger log = LoggerFactory.getLogger(FinanceService.class);
    private final NetWorthSnapshotRepository netWorthSnapshotRepository;
    private final AccountMonthlyBalanceRepository accountMonthlyBalanceRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final IncomeSourceRepository incomeSourceRepository;

    public FinanceService(
            NetWorthSnapshotRepository netWorthSnapshotRepository,
            AccountMonthlyBalanceRepository accountMonthlyBalanceRepository,
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            AccountTypeRepository accountTypeRepository,
            IncomeSourceRepository incomeSourceRepository) {
        this.netWorthSnapshotRepository = netWorthSnapshotRepository;
        this.accountMonthlyBalanceRepository = accountMonthlyBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.accountTypeRepository = accountTypeRepository;
        this.incomeSourceRepository = incomeSourceRepository;
    }

    private static final String SOURCE_HISTORICAL = "historical";
    private static final String SOURCE_ACCOUNT_BASED = "account-based";

    /** By date; on the same date, historical snapshots come before account-based entries. */
    private static final Comparator<NetWorthHistoryResponseDTO> HISTORY_ORDER = Comparator.comparing(
                    NetWorthHistoryResponseDTO::date)
            .thenComparing(entry -> SOURCE_HISTORICAL.equals(entry.source()) ? 0 : 1);

    public List<NetWorthHistoryResponseDTO> getCompleteNetWorthHistory() {
        Stream<NetWorthHistoryResponseDTO> historical =
                netWorthSnapshotRepository.findAllByOrderByBalanceMonthAsc().stream()
                        .map(snapshot -> new NetWorthHistoryResponseDTO(
                                snapshot.getBalanceMonth(), SOURCE_HISTORICAL, snapshot.getNetWorth(), null));

        Map<LocalDate, List<AccountBalanceResponseDTO>> accountsByDate =
                accountMonthlyBalanceRepository.findAllAccountBalancesWithDetails().stream()
                        .collect(Collectors.groupingBy(
                                AccountMonthlyBalance::getBalanceDate,
                                Collectors.mapping(FinanceService::toAccountBalanceDTO, Collectors.toList())));

        Stream<NetWorthHistoryResponseDTO> accountBased = accountsByDate.entrySet().stream()
                .map(entry -> new NetWorthHistoryResponseDTO(
                        entry.getKey(), SOURCE_ACCOUNT_BASED, netWorth(entry.getValue()), entry.getValue()));

        return Stream.concat(historical, accountBased).sorted(HISTORY_ORDER).toList();
    }

    private static AccountBalanceResponseDTO toAccountBalanceDTO(AccountMonthlyBalance balance) {
        Account account = balance.getAccount();
        return new AccountBalanceResponseDTO(
                account.getId(),
                account.getName(),
                account.getAccountType().getName(),
                account.getAccountType().getCategory().name(),
                balance.getBalance());
    }

    /** Assets minus liabilities, in a single pass. */
    private static BigDecimal netWorth(List<AccountBalanceResponseDTO> accounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (AccountBalanceResponseDTO account : accounts) {
            boolean isLiability = AccountCategory.LIABILITY.name().equals(account.category());
            total = isLiability ? total.subtract(account.balance()) : total.add(account.balance());
        }
        return total;
    }

    /** Creates or replaces the balance for one account and date (PUT is idempotent). */
    @Transactional
    public void updateAccountBalance(Integer accountId, LocalDate balanceDate, BigDecimal balance) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account", accountId);
        }
        accountMonthlyBalanceRepository.upsertBalance(accountId, balanceDate, balance);
        log.info("Saved balance for accountId={}, balanceDate={}", accountId, balanceDate);
    }

    /**
     * Starts a month from the one before it: every account balance from the
     * previous month is copied into {@code balanceDate}, except for accounts
     * that already have a balance there (those are kept as entered) and
     * archived accounts (no longer carried forward).
     */
    @Transactional
    public BalanceCopyResponseDTO copyPreviousMonthBalances(LocalDate balanceDate) {
        // Balances are stored on the 1st. Any other day would also make
        // minusMonths ambiguous (Mar 31 minus a month is Feb 28).
        if (balanceDate.getDayOfMonth() != 1) {
            throw new InvalidRequestException(
                    "balanceDate must be the first day of a month, e.g. " + balanceDate.withDayOfMonth(1));
        }
        LocalDate fromDate = balanceDate.minusMonths(1);
        if (!accountMonthlyBalanceRepository.existsActiveBalanceOn(fromDate)) {
            throw new InvalidRequestException(
                    "No active account balances for " + MONTH_YEAR.format(fromDate) + " to copy");
        }
        int copied = accountMonthlyBalanceRepository.copyBalances(fromDate, balanceDate);
        log.info("Copied {} balances from {} to {}", copied, fromDate, balanceDate);
        return new BalanceCopyResponseDTO(fromDate, balanceDate, copied);
    }

    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);

    public List<TransactionResponseDTO> getAllTransactions() {
        return transactionRepository.findAll(Sort.by(Sort.Order.desc("amount"), Sort.Order.asc("id"))).stream()
                .map(FinanceService::toTransactionDTO)
                .toList();
    }

    public List<AccountResponseDTO> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(FinanceService::toAccountDTO)
                .toList();
    }

    private static AccountResponseDTO toAccountDTO(Account account) {
        return new AccountResponseDTO(
                account.getId(),
                account.getName(),
                account.getAccountType().getName(),
                account.getAccountType().getCategory().toString(),
                account.isArchived());
    }

    /** The type with this name, created in {@code category} if it's new. */
    private AccountType findOrCreateType(String name, AccountCategory category) {
        return accountTypeRepository
                .findByName(name)
                .map(existing -> requireSameCategory(existing, category))
                .orElseGet(() -> {
                    log.debug("Creating account type '{}' ({})", name, category);
                    AccountType newAccountType = new AccountType();
                    newAccountType.setName(name);
                    newAccountType.setCategory(category);
                    return accountTypeRepository.save(newAccountType);
                });
    }

    /**
     * Renames, retypes or (un)archives an account. The category stays: the new
     * type must be one of the account's category, or a new name (created in it).
     * Name and type show in every month, so a rename applies to past months too;
     * balances and net worth don't change. Archiving keeps every balance.
     */
    @Transactional
    public AccountResponseDTO updateAccount(Integer accountId, AccountUpdateRequestDTO request) {
        Account account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        AccountCategory category = account.getAccountType().getCategory();

        account.setName(request.accountName().trim());
        account.setAccountType(findOrCreateType(request.accountType().trim(), category));
        account.setArchived(request.archived());
        Account saved = accountRepository.save(account);
        log.info(
                "Updated account id={} (type '{}', archived={})",
                saved.getId(),
                saved.getAccountType().getName(),
                saved.isArchived());
        return toAccountDTO(saved);
    }

    /**
     * Creates the account type (if new), the account, and optionally its first
     * balance. Because this runs in one transaction, a failure on any step
     * rolls back the earlier inserts, so no account is left without its balance.
     */
    @Transactional
    public AccountResponseDTO createAccount(AccountCreateRequestDTO request) {
        AccountCategory category = request.category();

        AccountType accountType = findOrCreateType(request.accountType(), category);

        Account account = new Account();
        account.setName(request.accountName());
        account.setAccountType(accountType);
        Account savedAccount = accountRepository.save(account);

        // Create initial monthly balance if provided
        if (request.balance() != null && request.balanceDate() != null) {
            AccountMonthlyBalance monthlyBalance = new AccountMonthlyBalance();
            monthlyBalance.setAccount(savedAccount);
            monthlyBalance.setBalance(request.balance());
            monthlyBalance.setBalanceDate(request.balanceDate());
            accountMonthlyBalanceRepository.save(monthlyBalance);
        }
        log.info(
                "Created account id={} (type '{}', initial balance: {})",
                savedAccount.getId(),
                accountType.getName(),
                request.balanceDate() != null ? request.balanceDate() : "none");

        return toAccountDTO(savedAccount);
    }

    /** Type names are unique, so "Mortgage" can't be both a LIABILITY and an ASSET. */
    private static AccountType requireSameCategory(AccountType existing, AccountCategory requested) {
        if (existing.getCategory() != requested) {
            throw new InvalidRequestException("Account type '" + existing.getName()
                    + "' already exists as " + existing.getCategory()
                    + "; choose a different type name for " + requested + " accounts");
        }
        return existing;
    }

    @Transactional
    public void deleteAccountBalance(Integer accountId, LocalDate balanceDate) {
        AccountMonthlyBalance balance = accountMonthlyBalanceRepository
                .findByAccountIdAndBalanceDate(accountId, balanceDate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account balance", "accountId=" + accountId + ", balanceDate=" + balanceDate));
        accountMonthlyBalanceRepository.delete(balance);
        log.info("Deleted balance for accountId={}, balanceDate={}", accountId, balanceDate);
    }

    @Transactional
    public TransactionResponseDTO createTransaction(TransactionCreateRequestDTO request) {
        Transaction transaction = new Transaction();
        transaction.setName(request.name().trim());
        transaction.setTransactionType(request.transactionType());
        transaction.setAmount(request.amount());
        transaction.setPaid(Boolean.TRUE.equals(request.paid()));
        transaction.setCash(Boolean.TRUE.equals(request.cash()));
        Transaction saved = transactionRepository.save(transaction);

        log.info("Created {} transaction id={}", saved.getTransactionType(), saved.getId());
        return toTransactionDTO(saved);
    }

    @Transactional
    public TransactionResponseDTO editTransaction(Integer id, TransactionUpdateRequestDTO request) {
        Transaction transaction =
                transactionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Transaction", id));

        transaction.setName(request.name().trim());
        transaction.setAmount(request.amount());
        transaction.setPaid(request.paid());
        if (request.cash() != null) {
            transaction.setCash(request.cash());
        }

        Transaction saved = transactionRepository.save(transaction);
        log.info("Updated transaction id={}", id);
        return toTransactionDTO(saved);
    }

    @Transactional
    public void deleteTransaction(Integer id) {
        Transaction transaction =
                transactionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        transactionRepository.delete(transaction);
        log.info("Deleted transaction id={}", id);
    }

    @Transactional
    public void clearAllTransactionsPaid() {
        int updated = transactionRepository.markAllUnpaid();
        log.info("Marked {} transactions as unpaid", updated);
    }

    public List<IncomeSourceResponseDTO> getAllIncomeSources() {
        return incomeSourceRepository.findAll().stream()
                .map(FinanceService::toIncomeSourceDTO)
                .toList();
    }

    @Transactional
    public IncomeSourceResponseDTO createIncomeSource(IncomeSourceCreateRequestDTO request) {
        IncomeSource incomeSource = new IncomeSource();
        incomeSource.setYear(request.year());
        incomeSource.setName(request.name().trim());
        incomeSource.setAmount(request.amount());
        IncomeSource saved = incomeSourceRepository.save(incomeSource);

        log.info("Created income source id={}", saved.getId());
        return toIncomeSourceDTO(saved);
    }

    @Transactional
    public IncomeSourceResponseDTO updateIncomeSource(Integer id, IncomeSourceUpdateRequestDTO request) {
        IncomeSource incomeSource = findIncomeSource(id);
        incomeSource.setName(request.name().trim());
        incomeSource.setAmount(request.amount());
        IncomeSource saved = incomeSourceRepository.save(incomeSource);
        log.info("Updated income source id={}", id);
        return toIncomeSourceDTO(saved);
    }

    @Transactional
    public void deleteIncomeSource(Integer id) {
        incomeSourceRepository.delete(findIncomeSource(id));
        log.info("Deleted income source id={}", id);
    }

    private IncomeSource findIncomeSource(Integer id) {
        return incomeSourceRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income source", id));
    }

    private static TransactionResponseDTO toTransactionDTO(Transaction transaction) {
        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getName(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.isPaid(),
                transaction.isCash());
    }

    private static IncomeSourceResponseDTO toIncomeSourceDTO(IncomeSource incomeSource) {
        return new IncomeSourceResponseDTO(
                incomeSource.getId(), incomeSource.getYear(), incomeSource.getName(), incomeSource.getAmount());
    }
}
