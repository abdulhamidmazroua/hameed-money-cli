package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AccountType;
import org.hameed.hameedmoneycli.model.AccountSpecification;
import org.hameed.hameedmoneycli.model.dto.AccountCreateDto;
import org.hameed.hameedmoneycli.model.dto.AccountFilter;

import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.repository.AccountRepository;
import org.hameed.hameedmoneycli.repository.IngestionRuleRepository;
import org.hameed.hameedmoneycli.repository.SourceSystemRepository;
import org.hameed.hameedmoneycli.repository.TransactionRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AssetService assetService;
    private final TransactionRepository transactionRepository;
    private final SourceSystemRepository sourceSystemRepository;
    private final IngestionRuleRepository ingestionRuleRepository;
    private final SystemAdjustmentService systemAdjustmentService;

    @Transactional
    public Account createAccount(AccountCreateDto newAccount) {

        Account parentAccount = null;
        if (newAccount.parentAccountId() != null) {
            parentAccount = accountRepository.findById(newAccount.parentAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent account with ID " + newAccount.parentAccountId() + " not found"));
        }
        Asset asset = null;
        if (newAccount.assetId() != null) {
            asset = assetService.getAssetById(newAccount.assetId());
        }

        String name = newAccount.name();
        if (asset != null && !name.startsWith(asset.getSymbol() + ":")) {
            name = asset.getSymbol() + ":" + name;
        }

        Account account = Account.builder()
                .name(name)
                .masterType(newAccount.accountType())
                .parent(parentAccount)
                .asset(asset)
                .isInternal(newAccount.isInternal())
                .build();
        accountRepository.save(account);

        if (asset != null) {
            createSystemAccounts(asset);
        }

        return account;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account with ID " + id + " not found"));
    }

    public Account getAccountByName(String name) {
        return accountRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new IllegalArgumentException("Account with name '" + name + "' not found"));
    }

    public List<Account> findAccounts(AccountFilter filter) {
        return accountRepository.findAll(
                Specification
                        .where(AccountSpecification.hasNameContaining(filter.keyword()))
                        .and(AccountSpecification.hasMasterType(filter.masterType()))
                        .and(AccountSpecification.hasAssetSymbol(filter.assetSymbol()))
        );
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = getAccountById(id);

        if (accountRepository.existsByParent_Id(id)) {
            throw new IllegalStateException("Cannot delete account '" + account.getName() + "': it has child accounts. Delete or reassign children first.");
        }
        if (transactionRepository.existsByFromAccount_IdOrToAccount_Id(id, id)) {
            throw new IllegalStateException("Cannot delete account '" + account.getName() + "': it has related transactions. Delete transactions first.");
        }
        if (sourceSystemRepository.existsByAnchoredAccount_Id(id)) {
            throw new IllegalStateException("Cannot delete account '" + account.getName() + "': it is anchored to a source system. Delete the source system first.");
        }
        if (ingestionRuleRepository.existsByTargetAccount_Id(id)) {
            throw new IllegalStateException("Cannot delete account '" + account.getName() + "': it is targeted by ingestion rules. Delete rules first.");
        }

        accountRepository.delete(account);
    }

    @Transactional
    public Account createAccountWithOpeningBalance(String name, Long parentAccountId, String assetSymbol, String balanceStr) {
        Asset asset = assetService.getAssetBySymbol(assetSymbol);

        Account account = createAccount(new AccountCreateDto(
                name,
                AccountType.ASSET,
                parentAccountId,
                asset.getId(),
                true
        ));

        systemAdjustmentService.openAccountBalance(account.getId(), new BigDecimal(balanceStr));
        return account;
    }

    private void createSystemAccounts(Asset asset) {
        if (accountRepository.existsByAsset_IdAndMasterType(asset.getId(), AccountType.SYSTEM)) {
            return;
        }

        String prefix = asset.getSymbol() + ":";
        accountRepository.save(Account.builder()
                .name(prefix + "Opening Balance")
                .masterType(AccountType.SYSTEM)
                .asset(asset)
                .isInternal(false)
                .build());
        accountRepository.save(Account.builder()
                .name(prefix + "Balance Increase Adjustment")
                .masterType(AccountType.SYSTEM)
                .asset(asset)
                .isInternal(false)
                .build());
        accountRepository.save(Account.builder()
                .name(prefix + "Balance Decrease Adjustment")
                .masterType(AccountType.SYSTEM)
                .asset(asset)
                .isInternal(false)
                .build());
    }


}
