package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.dto.RuleCreateDto;
import org.hameed.hameedmoneycli.model.entity.IngestionRule;
import org.hameed.hameedmoneycli.repository.IngestionRuleRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IngestionRuleService {

    private final IngestionRuleRepository ingestionRuleRepository;
    private final AccountService accountService;


    public void addRule(RuleCreateDto dto) {
        IngestionRule rule = IngestionRule.builder()
                .matchPattern(dto.matchPattern())
                .targetAccount(accountService.getAccountById(dto.targetAccountId()))
                .priority(ingestionRuleRepository.findMaxPriority() + 1)
                .build();
        ingestionRuleRepository.save(rule);
    }
}
