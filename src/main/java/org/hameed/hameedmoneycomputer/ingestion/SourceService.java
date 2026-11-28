package org.hameed.hameedmoneycomputer.ingestion;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.ledger.Account;
import org.hameed.hameedmoneycomputer.ledger.AccountStore;
import org.hameed.hameedmoneycomputer.ledger.SourceSystem;
import org.hameed.hameedmoneycomputer.ledger.SourceSystemStore;
import org.hameed.hameedmoneycomputer.shared.BusinessRuleException;
import org.hameed.hameedmoneycomputer.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceService {

    private final SourceSystemStore sources;
    private final AccountStore accounts;

    public List<SourceSystem> listSources() {
        return sources.findAll();
    }

    public SourceSystem getSourceByCode(String code) {
        return sources.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Source system not found: " + code));
    }

    public boolean existsByCode(String code) {
        return sources.existsByCode(code);
    }

    @Transactional
    public SourceSystem addSource(String name, String code, SourceFormatConfig formatConfig, Long anchoredAccountId) {
        if (sources.existsByCode(code)) {
            throw new BusinessRuleException("Source system code already exists: " + code);
        }

        Account anchored = null;
        if (anchoredAccountId != null) {
            anchored = accounts.findById(anchoredAccountId)
                    .orElseThrow(() -> new NotFoundException("Account not found: " + anchoredAccountId));
        }

        SourceSystem source = SourceSystem.builder()
                .name(name)
                .code(code)
                .formatConfig(formatConfig)
                .anchoredAccount(anchored)
                .build();
        return sources.save(source);
    }

    @Transactional
    public SourceSystem updateFormatConfig(String code, SourceFormatConfig formatConfig) {
        SourceSystem source = getSourceByCode(code);
        source.setFormatConfig(formatConfig);
        return sources.save(source);
    }

    @Transactional
    public SourceSystem updateAnchoredAccount(String code, Long accountId) {
        SourceSystem source = getSourceByCode(code);
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        source.setAnchoredAccount(account);
        return sources.save(source);
    }

    @Transactional
    public void removeSource(String code) {
        SourceSystem source = getSourceByCode(code);
        sources.delete(source);
    }
}
