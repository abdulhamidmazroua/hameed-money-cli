package org.hameed.hameedmoneycli.service;


import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.TransactionSpecification;
import org.hameed.hameedmoneycli.model.dto.TransactionCreateDto;
import org.hameed.hameedmoneycli.model.dto.TransactionFilter;
import org.hameed.hameedmoneycli.model.entity.SourceSystem;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.hameed.hameedmoneycli.repository.SourceSystemRepository;
import org.hameed.hameedmoneycli.repository.TransactionRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final SourceSystemRepository sourceSystemRepository;

    public void createTransaction(TransactionCreateDto transactionCreateDto) {
        var fromAcc = accountService.getAccountById(transactionCreateDto.fromAccountId());
        var toAcc = accountService.getAccountById(transactionCreateDto.toAccountId());
        if (fromAcc.getAsset() == null || toAcc.getAsset() == null) {
            throw new IllegalArgumentException(
                    "Both from and to accounts must be leaf accounts (non-null asset). Parent folders cannot post transactions.");
        }

        SourceSystem sourceSystem = sourceSystemRepository
                .findByCode(transactionCreateDto.sourceSystemCode().name())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown source system: " + transactionCreateDto.sourceSystemCode()));

        BigDecimal fee = transactionCreateDto.feeAmount() != null
                ? transactionCreateDto.feeAmount()
                : BigDecimal.ZERO;

        Transaction transaction = Transaction.builder()
                .description(transactionCreateDto.description())
                .type(transactionCreateDto.transactionType())
                .fromAccount(fromAcc)
                .toAccount(toAcc)
                .fromAmount(transactionCreateDto.fromAmount())
                .toAmount(transactionCreateDto.toAmount())
                .transactionDate(Instant.parse(transactionCreateDto.transactionDateTime()))
                .sourceSystem(sourceSystem)
                .feeAmount(fee)
                .isSystemAdjustment(false)
                .build();

        transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactions(TransactionFilter filter) {
        return transactionRepository.findAll(
                Specification.
                        where(TransactionSpecification.hasTransactionType(filter.transactionType()))
                        .and(TransactionSpecification.hasFromAccountId(filter.fromAccountId()))
                        .and(TransactionSpecification.hasToAccountId(filter.toAccountId()))
                        .and(TransactionSpecification.hasTransactionDateTimeFrom(filter.transactionDateTimeFrom())
                        .and(TransactionSpecification.hasTransactionDateTimeTo(filter.transactionDateTimeTo())))
        );
    }

}
