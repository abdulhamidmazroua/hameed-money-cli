package org.hameed.hameedmoneycli.repository;

import org.hameed.hameedmoneycli.enums.IngestedTransactionStatus;
import org.hameed.hameedmoneycli.model.entity.IngestedStagedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngestedStagedTransactionRepository extends JpaRepository<IngestedStagedTransaction, Long> {

    List<IngestedStagedTransaction> findBySessionIdOrderByRowIndex(Long sessionId);

    List<IngestedStagedTransaction> findBySessionIdAndStatusOrderByRowIndex(Long sessionId, IngestedTransactionStatus status);

    long countBySessionIdAndStatus(Long sessionId, IngestedTransactionStatus status);

    long countBySessionId(Long sessionId);

    long countBySessionIdAndParseErrorIsNotNull(Long sessionId);

    Optional<IngestedStagedTransaction> findBySessionIdAndRowIndex(Long sessionId, Integer rowIndex);
}
