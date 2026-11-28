package org.hameed.hameedmoneycomputer.store;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.ledger.Account;
import org.hameed.hameedmoneycomputer.ledger.AccountType;
import org.hameed.hameedmoneycomputer.ledger.Transaction;
import org.hameed.hameedmoneycomputer.ledger.TransactionFilter;
import org.hameed.hameedmoneycomputer.ledger.TransactionStore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class JpaTransactionStore implements TransactionStore {

    private final TransactionRepository repository;

    @Override
    public Transaction save(Transaction transaction) {
        return repository.save(transaction);
    }

    @Override
    public List<Transaction> find(TransactionFilter filter) {
        return repository.findAll(TransactionSpecification.matching(filter));
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public boolean existsByExternalRef(String externalRefId) {
        return repository.existsByExternalRefId(externalRefId);
    }

    @Override
    public boolean involvesAccount(long accountId) {
        return repository.existsByFromAccount_IdOrToAccount_Id(accountId, accountId);
    }

    @Override
    public long countForAccount(long accountId) {
        return repository.countByFromAccount_IdOrToAccount_Id(accountId, accountId);
    }

    @Override
    public Optional<TimeRange> dateRangeForAccount(long accountId) {
        List<Object[]> rows = repository.findDateRangeByAccountId(accountId);
        if (rows.isEmpty() || rows.getFirst()[0] == null) {
            return Optional.empty();
        }
        Object[] row = rows.getFirst();
        return Optional.of(new TimeRange(instant(row[0]), instant(row[1])));
    }

    @Override
    public BigDecimal balanceOf(long accountId) {
        BigDecimal balance = repository.getAccountBalance(accountId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    @Override
    public Optional<Instant> firstActivityDate() {
        return Optional.ofNullable(repository.findFirstActivityDate()).map(Instant::ofEpochMilli);
    }

    @Override
    public List<Transaction> findSystemAdjustments() {
        return repository.findAllSystemAdjustments();
    }

    @Override
    public BigDecimal totalVolume() {
        return repository.sumAllFromAmounts();
    }

    @Override
    public long countOrphanedFrom() {
        return repository.countOrphanFromAccounts();
    }

    @Override
    public long countOrphanedTo() {
        return repository.countOrphanToAccounts();
    }

    @Override
    public List<AccountFlow> netOutflowByAccountType(AccountType type, long fromMillis, long toExclusiveMillis) {
        return repository.sumNetOutflowByAccountType(type, fromMillis, toExclusiveMillis).stream()
                .map(row -> new AccountFlow((Account) row[0], decimal(row[1])))
                .toList();
    }

    private static Instant instant(Object epochMillis) {
        return Instant.ofEpochMilli(((Number) epochMillis).longValue());
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
    }
}
