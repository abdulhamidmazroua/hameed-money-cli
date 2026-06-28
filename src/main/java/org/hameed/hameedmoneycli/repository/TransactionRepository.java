package org.hameed.hameedmoneycli.repository;

import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    boolean existsByExternalRefId(String externalRefId);

    boolean existsByFromAccount_IdOrToAccount_Id(Long fromAccountId, Long toAccountId);

    @Query(value = "select coalesce(sum(case when to_account_id = :accountId then to_amount else 0 end) - sum(case when from_account_id = :accountId then from_amount else 0 end), 0) from transaction", nativeQuery = true)
    BigDecimal getAccountBalance(Long accountId);
}
