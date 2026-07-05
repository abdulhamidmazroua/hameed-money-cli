package org.hameed.hameedmoneycli.repository;

import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    boolean existsByExternalRefId(String externalRefId);

    boolean existsByFromAccount_IdOrToAccount_Id(Long fromAccountId, Long toAccountId);

    long countByFromAccount_IdOrToAccount_Id(Long fromAccountId, Long toAccountId);

    @Query("SELECT MIN(t.transactionDate), MAX(t.transactionDate) FROM Transaction t WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId")
    List<Object[]> findDateRangeByAccountId(Long accountId);

    @Query("SELECT t FROM Transaction t WHERE t.isSystemAdjustment = true")
    List<Transaction> findAllSystemAdjustments();

    @Query("SELECT COALESCE(SUM(t.fromAmount), 0) FROM Transaction t")
    BigDecimal sumAllFromAmounts();

    @Query(value = "SELECT COUNT(*) FROM transactions t LEFT JOIN accounts a ON t.from_account_id = a.id WHERE a.id IS NULL", nativeQuery = true)
    long countOrphanFromAccounts();

    @Query(value = "SELECT COUNT(*) FROM transactions t LEFT JOIN accounts a ON t.to_account_id = a.id WHERE a.id IS NULL", nativeQuery = true)
    long countOrphanToAccounts();

    @Query(value = "select coalesce(sum(case when to_account_id = :accountId then to_amount else 0 end) - sum(case when from_account_id = :accountId then from_amount else 0 end), 0) from transactions", nativeQuery = true)
    BigDecimal getAccountBalance(Long accountId);
}
