package org.hameed.hameedmoneycomputer.reporting;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.ledger.Account;
import org.hameed.hameedmoneycomputer.ledger.AccountStore;
import org.hameed.hameedmoneycomputer.ledger.AccountType;
import org.hameed.hameedmoneycomputer.ledger.AccountView;
import org.hameed.hameedmoneycomputer.ledger.TransactionStore;
import org.hameed.hameedmoneycomputer.shared.DateUtil;
import org.hameed.hameedmoneycomputer.shared.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Read-only consistency checks over the ledger. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AccountStore accounts;
    private final TransactionStore transactions;

    public AccountAudit auditAccount(long id) {
        Account account = accounts.findById(id)
                .orElseThrow(() -> new NotFoundException("Account with ID " + id + " not found"));
        Optional<TransactionStore.TimeRange> range = transactions.dateRangeForAccount(id);
        return new AccountAudit(
                AccountView.from(account),
                transactions.balanceOf(id),
                transactions.countForAccount(id),
                range.map(r -> DateUtil.toLocalDate(r.first())).orElse(null),
                range.map(r -> DateUtil.toLocalDate(r.last())).orElse(null));
    }

    public LedgerAudit auditTrail() {
        List<Account> leaves = accounts.findLeaves();
        List<LedgerAudit.NegativeBalance> negatives = new ArrayList<>();
        for (Account account : leaves) {
            if (account.getMasterType() != AccountType.ASSET) {
                continue;
            }
            BigDecimal balance = transactions.balanceOf(account.getId());
            if (balance.signum() < 0) {
                negatives.add(new LedgerAudit.NegativeBalance(account.getId(), account.getName(), balance));
            }
        }
        return new LedgerAudit(leaves.size(), transactions.count(), negatives,
                transactions.countOrphanedFrom(), transactions.countOrphanedTo());
    }
}
