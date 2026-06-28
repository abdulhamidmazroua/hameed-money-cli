package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.hameed.hameedmoneycli.repository.AccountRepository;
import org.hameed.hameedmoneycli.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Africa/Cairo");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(DEFAULT_ZONE);

    public String auditAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account with ID " + id + " not found"));

        BigDecimal balance = transactionRepository.getAccountBalance(account.getId());
        long txCount = transactionRepository.countByFromAccount_IdOrToAccount_Id(account.getId(), account.getId());
        List<Object[]> dateRange = transactionRepository.findDateRangeByAccountId(account.getId());
        String dateRangeStr = "N/A";
        if (!dateRange.isEmpty() && dateRange.get(0)[0] != null) {
            dateRangeStr = DATE_FMT.format((Instant) dateRange.get(0)[0]) + " to " + DATE_FMT.format((Instant) dateRange.get(0)[1]);
        }

        String assetLabel = account.getAsset() == null ? "(none)" : account.getAsset().getName() + " (" + account.getAsset().getSymbol() + ")";

        return """
                --- AUDIT ACCOUNT ---
                Account:   %s (ID: %d)
                Asset:     %s
                Type:      %s | Internal: %s
                Balance:   %s
                Transactions: %d (%s)
                """.formatted(
                account.getName(), account.getId(),
                assetLabel,
                account.getMasterType(), account.getIsInternal(),
                balance.toPlainString(),
                txCount, dateRangeStr
        );
    }

    public String auditTrail() {
        List<Account> leafAccounts = accountRepository.getLeafAccounts();
        long totalTx = transactionRepository.count();

        int anomalyCount = 0;
        StringBuilder anomalies = new StringBuilder();

        for (Account account : leafAccounts) {
            BigDecimal balance = transactionRepository.getAccountBalance(account.getId());

            if (account.getMasterType() == org.hameed.hameedmoneycli.enums.AccountType.ASSET && balance.compareTo(BigDecimal.ZERO) < 0) {
                anomalyCount++;
                anomalies.append("  - NEGATIVE BALANCE: ").append(account.getName())
                        .append(" (ID: ").append(account.getId()).append(") = ").append(balance).append("\n");
            }
        }

        // Orphan check
        long orphanFrom = transactionRepository.countOrphanFromAccounts();
        long orphanTo = transactionRepository.countOrphanToAccounts();
        long orphanTotal = orphanFrom + orphanTo;
        if (orphanTotal > 0) {
            anomalyCount++;
            anomalies.append("  - ORPHANED TRANSACTIONS: ").append(orphanTotal)
                    .append(" (from: ").append(orphanFrom).append(", to: ").append(orphanTo).append(")\n");
        }

        String result;
        if (anomalyCount == 0) {
            result = "All accounts verified \u2713";
        } else {
            result = "Anomalies found: " + anomalyCount + "\n" + anomalies.toString().stripTrailing();
        }

        return """
                --- AUDIT TRAIL ---
                Leaf accounts: %d
                Total transactions: %d
                
                %s
                """.formatted(leafAccounts.size(), totalTx, result);
    }
}
