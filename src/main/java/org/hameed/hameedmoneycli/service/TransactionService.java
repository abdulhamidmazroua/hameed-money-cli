package org.hameed.hameedmoneycli.service;


import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.TransactionSpecification;
import org.hameed.hameedmoneycli.model.dto.TransactionCreateDto;
import org.hameed.hameedmoneycli.model.dto.TransactionDto;
import org.hameed.hameedmoneycli.model.dto.TransactionFilter;
import org.hameed.hameedmoneycli.model.entity.SourceSystem;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.hameed.hameedmoneycli.repository.SourceSystemRepository;
import org.hameed.hameedmoneycli.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final SourceSystemRepository sourceSystemRepository;

    @Value("${hmc.report.output-dir}")
    private String transactionReportPath;

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

    public void generateTransactionReport(TransactionFilter filter) throws IOException {
        // generate a csv file in a path defined in the application properties file with the transactions matching the filter criteria
        List<Transaction> transactions = this.getTransactions(filter);

        String fullPathStr = System.getProperty("user.home") + transactionReportPath + "/transaction_report-"+ OffsetDateTime.now() + ".csv";
        Path path = Path.of(fullPathStr);
        Files.createDirectories(path.getParent());
        Files.write(path, "description,type,transaction date,from account,from amount,to account,to amount,fee amount,source system,record creation date\n".getBytes());
        transactions.stream()
            .map(this::convertToDto)
            .forEach(dto -> {
                String line = String.join(",",
                        dto.description(),
                        dto.type(),
                        dto.transactionDateTime(),
                        dto.fromAccountName() + " " + dto.forAssetCode(),
                        dto.fromAmount(),
                        dto.toAccountName() + " " + dto.toAssetCode(),
                        dto.toAmount(),
                        dto.feeAmount(),
                        dto.sourceSystemName() != null ? dto.sourceSystemName() : "",
                        dto.createdAt()
                );
                try {
                    Files.writeString(path, line + System.lineSeparator(), Files.exists(path) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write transaction report: " + e.getMessage(), e);
                }
            });
    }

    private TransactionDto convertToDto(Transaction transaction) {
        return new TransactionDto(
                transaction.getDescription(),
                transaction.getType().name(),
                transaction.getTransactionDate().toString(),
                transaction.getFromAccount().getName(),
                transaction.getFromAccount().getAsset().getSymbol(),
                transaction.getFromAmount().toPlainString(),
                transaction.getToAccount().getName(),
                transaction.getToAccount().getAsset().getSymbol(),
                transaction.getToAmount().toPlainString(),
                transaction.getFeeAmount() != null ? transaction.getFeeAmount().toPlainString() : "0",
                transaction.getSourceSystem() != null ? transaction.getSourceSystem().getName() : null,
                transaction.getCreatedAt().toString()
        );
    }

}
