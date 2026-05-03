package org.hameed.hameedmoneycli.service;


import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.dto.TransactionCreateDto;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.hameed.hameedmoneycli.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public void createTransaction(TransactionCreateDto transactionCreateDto) {
        Transaction transaction = Transaction.builder()
                .description(transactionCreateDto.description())
                .type(transactionCreateDto.transactionType())
                .fromAccount(accountService.getAccountById(transactionCreateDto.fromAccountId()))
                .toAccount(accountService.getAccountById(transactionCreateDto.toAccountId()))
                .fromAmount(transactionCreateDto.fromAmount())
                .toAmount(transactionCreateDto.toAmount())
                .transactionDate(OffsetDateTime.parse(transactionCreateDto.transactionDateTime()))
                .sourceSystem(transactionCreateDto.sourceSystem())
                .feeAmount(transactionCreateDto.feeAmount())
                .build();

        transactionRepository.save(transaction);
    }

}
