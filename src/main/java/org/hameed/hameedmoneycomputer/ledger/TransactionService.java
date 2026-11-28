package org.hameed.hameedmoneycomputer.ledger;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.shared.BusinessRuleException;
import org.hameed.hameedmoneycomputer.shared.DateUtil;
import org.hameed.hameedmoneycomputer.shared.NotFoundException;
import org.hameed.hameedmoneycomputer.shared.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Posting and querying transactions between leaf accounts. */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionStore transactions;
    private final AccountStore accounts;
    private final SourceSystemStore sources;

    @Transactional
    public TransactionView createTransaction(TransactionCreateDto dto) {
        Account from = requireAccount(dto.fromAccountId());
        Account to = requireAccount(dto.toAccountId());
        if (from.getAsset() == null || to.getAsset() == null) {
            throw new BusinessRuleException(
                    "Both from and to accounts must be leaf accounts (non-null asset). Parent folders cannot post transactions.");
        }
        if (dto.transactionType() == null) {
            throw new ValidationException("Transaction type is required");
        }
        if (dto.transactionDate() == null) {
            throw new ValidationException("Transaction date is required");
        }
        requirePositive(dto.fromAmount(), "From amount");
        requirePositive(dto.toAmount(), "To amount");
        BigDecimal fee = dto.feeAmount() != null ? dto.feeAmount() : BigDecimal.ZERO;
        if (fee.signum() < 0) {
            throw new ValidationException("Fee must not be negative");
        }

        SourceSystem source = sources.findByCode(dto.sourceSystemCode())
                .orElseThrow(() -> new NotFoundException("Unknown source system: " + dto.sourceSystemCode()));

        Transaction transaction = transactions.save(Transaction.builder()
                .description(dto.description() == null ? "" : dto.description())
                .type(dto.transactionType())
                .fromAccount(from)
                .toAccount(to)
                .fromAmount(dto.fromAmount())
                .toAmount(dto.toAmount())
                .transactionDate(DateUtil.startOfDayMillis(dto.transactionDate()))
                .sourceSystem(source)
                .feeAmount(fee)
                .isSystemAdjustment(false)
                .build());
        return TransactionView.from(transaction);
    }

    public List<TransactionView> getTransactions(TransactionFilter filter) {
        return transactions.find(filter).stream().map(TransactionView::from).toList();
    }

    private Account requireAccount(Long id) {
        if (id == null) {
            throw new ValidationException("Both from and to accounts are required");
        }
        return accounts.findById(id)
                .orElseThrow(() -> new NotFoundException("Account with ID " + id + " not found"));
    }

    private static void requirePositive(BigDecimal amount, String label) {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException(label + " must be greater than zero");
        }
    }
}
