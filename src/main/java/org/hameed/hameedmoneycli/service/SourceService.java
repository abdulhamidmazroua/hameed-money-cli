package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.SourceFormatConfig;
import org.hameed.hameedmoneycli.model.entity.SourceSystem;
import org.hameed.hameedmoneycli.repository.AccountRepository;
import org.hameed.hameedmoneycli.repository.SourceSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceService {

    private final SourceSystemRepository sourceSystemRepository;
    private final AccountRepository accountRepository;

    public List<SourceSystem> listSources() {
        return sourceSystemRepository.findAll();
    }

    public SourceSystem getSourceByCode(String code) {
        return sourceSystemRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Source system not found: " + code));
    }

    public boolean existsByCode(String code) {
        return sourceSystemRepository.existsByCode(code);
    }

    @Transactional
    public SourceSystem addSource(String name, String code, SourceFormatConfig formatConfig, Long anchoredAccountId) {
        if (sourceSystemRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Source system code already exists: " + code);
        }

        Account anchored = null;
        if (anchoredAccountId != null) {
            anchored = accountRepository.findById(anchoredAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + anchoredAccountId));
        }

        SourceSystem source = SourceSystem.builder()
                .name(name)
                .code(code)
                .formatConfig(formatConfig)
                .anchoredAccount(anchored)
                .build();
        return sourceSystemRepository.save(source);
    }

    @Transactional
    public SourceSystem updateFormatConfig(String code, SourceFormatConfig formatConfig) {
        SourceSystem source = getSourceByCode(code);
        source.setFormatConfig(formatConfig);
        return sourceSystemRepository.save(source);
    }

    @Transactional
    public SourceSystem updateAnchoredAccount(String code, Long accountId) {
        SourceSystem source = getSourceByCode(code);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        source.setAnchoredAccount(account);
        return sourceSystemRepository.save(source);
    }

    @Transactional
    public void removeSource(String code) {
        SourceSystem source = getSourceByCode(code);
        sourceSystemRepository.delete(source);
    }

    public List<Account> getLeafAccounts() {
        return accountRepository.findAll().stream()
                .filter(a -> a.getAsset() != null)
                .toList();
    }
}
