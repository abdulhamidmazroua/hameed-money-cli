package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.SourceSystemCode;
import org.hameed.hameedmoneycli.enums.TransactionType;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.model.entity.SourceSystem;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.hameed.hameedmoneycli.repository.AccountRepository;
import org.hameed.hameedmoneycli.repository.SourceSystemRepository;
import org.hameed.hameedmoneycli.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SystemAdjustmentService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SourceSystemRepository sourceSystemRepository;

    @Transactional
    public void adjustBalance(Long accountId, BigDecimal actualBalance) {
        Account leaf = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account with ID " + accountId + " not found"));

        Asset asset = leaf.getAsset();
        if (asset == null) {
            throw new IllegalArgumentException("Account '" + leaf.getName() + "' is a folder (no asset). Cannot reconcile.");
        }

        BigDecimal currentBalance = transactionRepository.getAccountBalance(leaf.getId());
        BigDecimal difference = actualBalance.subtract(currentBalance);

        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        SourceSystem sourceSystem = sourceSystemRepository.findByCode(SourceSystemCode.MANUAL_ENTRY.name())
                .orElseThrow(() -> new IllegalStateException("MANUAL_ENTRY source system not found in seed data."));
        BigDecimal absDiff = difference.abs();

        Transaction tx;
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            Account increaseAccount = accountRepository.findByNameIgnoreCase(asset.getSymbol() + ":Balance Increase Adjustment")
                    .orElseThrow(() -> new IllegalStateException(
                            "No increase adjustment SYSTEM account found for asset " + asset.getSymbol()));

            tx = Transaction.builder()
                    .description("Balance increase adjustment for " + leaf.getName() + " (actual: " + actualBalance + ")")
                    .type(TransactionType.SYSTEM_ADJUSTMENT)
                    .fromAccount(increaseAccount)
                    .toAccount(leaf)
                    .fromAmount(absDiff)
                    .toAmount(absDiff)
                    .transactionDate(Instant.now())
                    .sourceSystem(sourceSystem)
                    .feeAmount(BigDecimal.ZERO)
                    .isSystemAdjustment(true)
                    .build();
        } else {
            Account decreaseAccount = accountRepository.findByNameIgnoreCase(asset.getSymbol() + ":Balance Decrease Adjustment")
                    .orElseThrow(() -> new IllegalStateException(
                            "No decrease adjustment SYSTEM account found for asset " + asset.getSymbol()));

            tx = Transaction.builder()
                    .description("Balance decrease adjustment for " + leaf.getName() + " (actual: " + actualBalance + ")")
                    .type(TransactionType.SYSTEM_ADJUSTMENT)
                    .fromAccount(leaf)
                    .toAccount(decreaseAccount)
                    .fromAmount(absDiff)
                    .toAmount(absDiff)
                    .transactionDate(Instant.now())
                    .sourceSystem(sourceSystem)
                    .feeAmount(BigDecimal.ZERO)
                    .isSystemAdjustment(true)
                    .build();
        }

        transactionRepository.save(tx);
    }

    @Transactional
    public void openAccountBalance(Long accountId, BigDecimal balance) {
        Account leaf = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account with ID " + accountId + " not found"));

        Asset asset = leaf.getAsset();
        if (asset == null) {
            throw new IllegalArgumentException("Account '" + leaf.getName() + "' is a folder (no asset). Cannot post an opening balance.");
        }

        Account openingAccount = accountRepository.findByNameIgnoreCase(asset.getSymbol() + ":Opening Balance")
                .orElseThrow(() -> new IllegalStateException(
                        "No opening balance SYSTEM account found for asset " + asset.getSymbol() + ". " +
                        "Create a leaf account with this asset first so the SYSTEM trio is generated."));

        SourceSystem sourceSystem = sourceSystemRepository.findByCode(SourceSystemCode.MANUAL_ENTRY.name())
                .orElseThrow(() -> new IllegalStateException("MANUAL_ENTRY source system not found in seed data."));

        Transaction tx = Transaction.builder()
                .description("Opening balance for " + leaf.getName())
                .type(TransactionType.SYSTEM_ADJUSTMENT)
                .fromAccount(openingAccount)
                .toAccount(leaf)
                .fromAmount(balance)
                .toAmount(balance)
                .transactionDate(Instant.now())
                .sourceSystem(sourceSystem)
                .feeAmount(BigDecimal.ZERO)
                .isSystemAdjustment(true)
                .build();

        transactionRepository.save(tx);
    }
}
