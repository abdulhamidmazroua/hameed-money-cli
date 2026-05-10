package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.dto.AccountCreateDto;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AssetService assetService;


    public void createAccount(AccountCreateDto newAccount) {

        Account parentAccount = null;
        if (newAccount.parentAccountId() != null) {
            parentAccount = accountRepository.findById(newAccount.parentAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent account with ID " + newAccount.parentAccountId() + " not found"));
        }
        Asset asset = null;
        if (newAccount.assetId() != null) {
            asset = assetService.getAssetById(newAccount.assetId());
        }

        Account account = Account.builder()
                .name(newAccount.name())
                .masterType(newAccount.accountType())
                .parent(parentAccount)
                .asset(asset)
                .isInternal(newAccount.isInternal())
                .build();
        accountRepository.save(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account with ID " + id + " not found"));
    }


}

