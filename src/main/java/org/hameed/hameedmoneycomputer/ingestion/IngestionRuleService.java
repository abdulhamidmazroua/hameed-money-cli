package org.hameed.hameedmoneycomputer.ingestion;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.ledger.Account;
import org.hameed.hameedmoneycomputer.ledger.AccountReferences;
import org.hameed.hameedmoneycomputer.ledger.AccountStore;
import org.hameed.hameedmoneycomputer.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IngestionRuleService implements AccountReferences {

    private final IngestionRuleRepository ingestionRuleRepository;
    private final AccountStore accounts;

    @Transactional
    public void addRule(RuleCreateDto dto) {
        Account target = accounts.findById(dto.targetAccountId())
                .orElseThrow(() -> new NotFoundException("Account with ID " + dto.targetAccountId() + " not found"));
        IngestionRule rule = IngestionRule.builder()
                .matchPattern(dto.matchPattern())
                .targetAccount(target)
                .priority(ingestionRuleRepository.findMaxPriority() + 1)
                .build();
        ingestionRuleRepository.save(rule);
    }

    /** Accounts targeted by ingestion rules can't be deleted. */
    @Override
    public Optional<String> blockingReason(Account account) {
        return ingestionRuleRepository.existsByTargetAccount_Id(account.getId())
                ? Optional.of("it is targeted by ingestion rules. Delete rules first.")
                : Optional.empty();
    }
}
