package org.hameed.hameedmoneycli.repository;

import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.IngestionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IngestionRuleRepository extends JpaRepository<IngestionRule, Long> {

    /** Higher {@code priority} first; ties broken by id for stable ordering. */
    @Query("SELECT r FROM IngestionRule r JOIN FETCH r.targetAccount ORDER BY r.priority DESC, r.id ASC")
    List<IngestionRule> findAllOrderedForMatching();

    @Query("SELECT COALESCE(MAX(r.priority), 0) FROM IngestionRule r")
    int findMaxPriority();

    boolean existsByTargetAccount_Id(Long accountId);

    boolean existsByMatchPatternAndTargetAccount(String matchPattern, Account targetAccount);
}
