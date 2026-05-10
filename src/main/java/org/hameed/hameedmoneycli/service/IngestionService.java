package org.hameed.hameedmoneycli.service;

import org.hameed.hameedmoneycli.model.entity.SourceSystem;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.hameed.hameedmoneycli.repository.SourceSystemRepository;
import org.hameed.hameedmoneycli.repository.TransactionRepository;
import org.hameed.hameedmoneycli.util.IngestionStrategy;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class IngestionService {

    private final Map<String, IngestionStrategy> ingestionStrategyMap;
    private final TransactionRepository transactionRepository;
    private final SourceSystemRepository sourceSystemRepository;

    public IngestionService(Map<String, IngestionStrategy> ingestionStrategyMap,
                            TransactionRepository transactionRepository,
                            SourceSystemRepository sourceSystemRepository) {
        this.ingestionStrategyMap = ingestionStrategyMap;
        this.transactionRepository = transactionRepository;
        this.sourceSystemRepository = sourceSystemRepository;
    }

    @Transactional
    public void ingestTransactions(String source, String filePath, CommandContext ctx) throws IOException {
        IngestionStrategy strategy = ingestionStrategyMap.get(source);
        if (strategy == null) {
            throw new IllegalArgumentException("No ingestion strategy found for source: " + source);
        }
        SourceSystem sourceSystem = sourceSystemRepository.findByCode(source)
                .orElseThrow(() -> new IllegalArgumentException("Unknown source system code: " + source));

        List<Transaction> transactionList = strategy.ingest(filePath, sourceSystem, ctx);
        if (!transactionList.isEmpty()) {
            transactionRepository.saveAll(transactionList);
        }
    }
}
