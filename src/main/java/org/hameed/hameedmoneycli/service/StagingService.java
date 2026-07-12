package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.classifier.Classifier;
import org.hameed.hameedmoneycli.classifier.RuleClassifier;
import org.hameed.hameedmoneycli.model.entity.SourceFormatConfig;
import org.hameed.hameedmoneycli.enums.IngestedTransactionStatus;
import org.hameed.hameedmoneycli.enums.StagingSessionStatus;
import org.hameed.hameedmoneycli.enums.TransactionType;
import org.hameed.hameedmoneycli.ingestion.GenericParser;
import org.hameed.hameedmoneycli.ingestion.IngestionUtil;
import org.hameed.hameedmoneycli.model.entity.*;
import org.hameed.hameedmoneycli.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

import static org.hameed.hameedmoneycli.constants.CommandConstants.*;

@Service
@RequiredArgsConstructor
public class StagingService {

    private final SourceSystemRepository sourceSystemRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IngestionStagingSessionRepository sessionRepository;
    private final IngestedStagedTransactionRepository stagedRepository;
    private final RuleClassifier ruleClassifier;
    private final List<Classifier> classifiers;
    private final IngestionRuleRepository ingestionRuleRepository;

    @Transactional
    public StagingResult parse(String sourceCode, String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        SourceSystem sourceSystem = sourceSystemRepository.findByCode(sourceCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown source system code: " + sourceCode));

        SourceFormatConfig sourceConfig = sourceSystem.getFormatConfig();
        if (sourceConfig == null) {
            throw new IllegalArgumentException("No format config for source '" + sourceCode + "'. Use 'source update-format' to add one.");
        }

        Account anchored = null;
        if (sourceSystem.getAnchoredAccount() != null) {
            anchored = accountRepository.findById(sourceSystem.getAnchoredAccount().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Anchored account not found for source system " + sourceCode));
        }

        List<GenericParser.ParsedRow> parsed = GenericParser.parse(filePath, sourceConfig);

        String fileHash = fileHash(path);

        IngestionStagingSession session = IngestionStagingSession.builder()
                .sourceCode(sourceCode)
                .anchoredAccount(anchored)
                .fileName(path.getFileName().toString())
                .fileHash(fileHash)
                .totalRows(parsed.size())
                .status(StagingSessionStatus.PARSED)
                .build();
        session = sessionRepository.save(session);

        int duplicateCount = 0;
        List<IngestedStagedTransaction> stagedRows = new ArrayList<>();

        for (GenericParser.ParsedRow row : parsed) {
            String externalRef = IngestionUtil.externalRefId(
                    sourceCode, row.rawDate(), row.rawDescription(), row.rawAmount());

            boolean isDuplicate = !row.hasError() && transactionRepository.existsByExternalRefId(externalRef);

            String parseError = row.hasError() ? row.errorMessage() : null;

            IngestedStagedTransaction staged = IngestedStagedTransaction.builder()
                    .session(session)
                    .rowIndex(row.rowIndex())
                    .status(IngestedTransactionStatus.PENDING)
                    .rawDate(row.rawDate())
                    .rawDescription(row.rawDescription())
                    .rawAmount(row.rawAmount())
                    .parsedDate(row.parsedDate())
                    .parsedAmount(row.parsedAmount())
                    .parseError(parseError)
                    .build();

            if (isDuplicate) {
                staged.setStatus(IngestedTransactionStatus.DUPLICATE);
                duplicateCount++;
            }

            // Build metadata map
            Map<String, String> meta = new HashMap<>();
            meta.put("amount", row.rawAmount());
            meta.put("description", row.rawDescription());
            staged.setMetadata(meta);

            stagedRows.add(staged);
        }

        stagedRepository.saveAll(stagedRows);

        autoClassify(session.getId());

        Map<String, Long> stats = getSessionStats(session.getId());
        return new StagingResult(
                session.getId(),
                stagedRows.size(),
                stats.getOrDefault("pending", 0L).intValue(),
                stats.getOrDefault("errors", 0L).intValue(),
                stats.getOrDefault("duplicate", 0L).intValue(),
                stats.getOrDefault("classified", 0L).intValue()
        );
    }

    @Transactional
    public void autoClassify(Long sessionId) {
        List<IngestedStagedTransaction> pending = stagedRepository
                .findBySessionIdAndStatusOrderByRowIndex(sessionId, IngestedTransactionStatus.PENDING);

        List<IngestedStagedTransaction> classifiable = pending.stream()
                .filter(r -> !r.hasError())
                .toList();

        List<Account> leafAccounts = accountRepository.findAll().stream()
                .filter(a -> a.getAsset() != null)
                .toList();

        // Rules first — single row at a time
        for (IngestedStagedTransaction row : classifiable) {
            var result = ruleClassifier.classify(row, leafAccounts);
            if (result.isPresent()) {
                var c = result.get();
                row.setClassifier(ruleClassifier.name());
                row.setSuggestedAccount(c.account());
                row.setSuggestedTxType(c.transactionType());
                row.setConfidence(c.confidence());
                row.setLlmReasoning(c.reasoning());
                row.setStatus(IngestedTransactionStatus.CLASSIFIED);
            }
        }

        // LLM bulk classify remaining unclassified rows
        List<IngestedStagedTransaction> unclassified = classifiable.stream()
                .filter(r -> !IngestedTransactionStatus.CLASSIFIED.equals(r.getStatus()))
                .toList();

        if (!unclassified.isEmpty()) {
            classifiers.stream()
                    .filter(c -> c instanceof org.hameed.hameedmoneycli.classifier.LLMClassifier)
                    .map(c -> (org.hameed.hameedmoneycli.classifier.LLMClassifier) c)
                    .findFirst()
                    .ifPresent(llm -> llm.bulkClassify(unclassified, leafAccounts));
        }

        stagedRepository.saveAll(pending);
    }

    @Transactional
    public ApplyResult apply(Long sessionId) {
        IngestionStagingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        List<IngestedStagedTransaction> classified = stagedRepository
                .findBySessionIdAndStatusOrderByRowIndex(sessionId, IngestedTransactionStatus.CLASSIFIED);

        SourceSystem sourceSystem = sourceSystemRepository.findByCode(session.getSourceCode())
                .orElseThrow(() -> new IllegalStateException("Source system not found: " + session.getSourceCode()));

        Account anchored = session.getAnchoredAccount();

        List<Transaction> transactions = new ArrayList<>();
        int applied = 0;
        int skipped = 0;

        for (IngestedStagedTransaction row : classified) {
            Account counterparty = row.effectiveAccount();
            BigDecimal amount = row.effectiveAmount();
            String description = row.effectiveDescription().length() > 500
                    ? row.effectiveDescription().substring(0, 500)
                    : row.effectiveDescription();

            String externalRef = IngestionUtil.externalRefId(
                    session.getSourceCode(), row.getRawDate(), row.getRawDescription(), row.getRawAmount());

            if (transactionRepository.existsByExternalRefId(externalRef)) {
                row.setStatus(IngestedTransactionStatus.DUPLICATE);
                skipped++;
                continue;
            }

            if (anchored.getAsset() == null || counterparty.getAsset() == null) {
                row.setParseError("Either anchored account or counterparty has no asset");
                skipped++;
                continue;
            }

            BigDecimal magnitude = amount.abs();
            Account fromAccount = amount.signum() > 0 ? counterparty : anchored;
            Account toAccount = amount.signum() > 0 ? anchored : counterparty;

            Transaction tx = Transaction.builder()
                    .description(description)
                    .type(parseTransactionType(row.getSuggestedTxType()))
                    .transactionDate(row.getParsedDate())
                    .fromAccount(fromAccount)
                    .toAccount(toAccount)
                    .fromAmount(magnitude)
                    .toAmount(magnitude)
                    .feeAmount(BigDecimal.ZERO)
                    .externalRefId(externalRef)
                    .sourceSystem(sourceSystem)
                    .isSystemAdjustment(false)
                    .metadata(row.getMetadata())
                    .build();

            tx = transactionRepository.save(tx);
            row.setAppliedTransaction(tx);
            row.setStatus(IngestedTransactionStatus.APPLIED);
            applied++;
            transactions.add(tx);

            String descForRule = row.effectiveDescription();
            if (descForRule != null && !descForRule.isBlank()) {
                Account ruleAccount = row.effectiveAccount();
                if (ruleAccount != null) {
                    String keyword = extractRuleKeyword(descForRule);
                    String pattern = "(?i).*" + Pattern.quote(keyword) + ".*";
                    if (!ingestionRuleRepository.existsByMatchPatternAndTargetAccount(pattern, ruleAccount)) {
                        ingestionRuleRepository.save(IngestionRule.builder()
                                .matchPattern(pattern)
                                .targetAccount(ruleAccount)
                                .priority(100)
                                .build());
                    }
                }
            }
        }

        stagedRepository.saveAll(classified);

        long remainingPending = stagedRepository.countBySessionIdAndStatus(sessionId, IngestedTransactionStatus.PENDING);
        long discarded = stagedRepository.countBySessionIdAndStatus(sessionId, IngestedTransactionStatus.DISCARDED);

        session.setStatus(remainingPending == 0 ? StagingSessionStatus.APPLIED : StagingSessionStatus.PARTIALLY_APPLIED);
        sessionRepository.save(session);

        return new ApplyResult(sessionId, applied, skipped, (int) remainingPending, (int) discarded);
    }

    @Transactional
    public void editRow(Long sessionId, Integer rowIndex, String field, String value) {
        IngestedStagedTransaction row = stagedRepository.findBySessionIdAndRowIndex(sessionId, rowIndex)
                .orElseThrow(() -> new IllegalArgumentException("Row not found: session " + sessionId + ", row " + rowIndex));

        switch (field) {
            case "account" -> row.setOverrideAccount(resolveAccount(value));
            case "description" -> row.setOverrideDescription(value);
            case "amount" -> {
                try {
                    row.setOverrideAmount(new BigDecimal(value));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(String.format(INGEST_EDIT_INVALID_AMOUNT, value));
                }
            }
            case "status" -> {
                try {
                    row.setStatus(IngestedTransactionStatus.valueOf(value.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(String.format(INGEST_EDIT_INVALID_STATUS, value));
                }
            }
            case "notes" -> row.setUserNotes(value);
            default -> throw new IllegalArgumentException(String.format(INGEST_EDIT_UNKNOWN_FIELD, field));
        }

        stagedRepository.save(row);
    }

    private Account resolveAccount(String value) {
        try {
            Long id = Long.parseLong(value);
            return accountRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found by ID: " + id));
        } catch (NumberFormatException e) {
            return accountRepository.findByNameIgnoreCase(value)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found by name: " + value));
        }
    }

    @Transactional
    public void discard(Long sessionId, Long rowId) {
        if (rowId != null) {
            IngestedStagedTransaction row = stagedRepository.findById(rowId)
                    .orElseThrow(() -> new IllegalArgumentException("Staged row not found: " + rowId));
            row.setStatus(IngestedTransactionStatus.DISCARDED);
            stagedRepository.save(row);
        } else {
            List<IngestedStagedTransaction> rows = stagedRepository
                .findBySessionIdAndStatusOrderByRowIndex(sessionId, IngestedTransactionStatus.PENDING);
            for (IngestedStagedTransaction row : rows) {
            row.setStatus(IngestedTransactionStatus.DISCARDED);
            }
            stagedRepository.saveAll(rows);

            IngestionStagingSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            session.setStatus(StagingSessionStatus.CANCELLED);
            sessionRepository.save(session);
        }
    }

    public List<IngestionStagingSession> listSessions() {
        return sessionRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<IngestedStagedTransaction> getStagedRows(Long sessionId, IngestedTransactionStatus status) {
        if (status != null) {
            return stagedRepository.findBySessionIdAndStatusOrderByRowIndex(sessionId, status);
        }
        return stagedRepository.findBySessionIdOrderByRowIndex(sessionId);
    }

    public IngestionStagingSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    public Map<String, Long> getSessionStats(Long sessionId) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", stagedRepository.countBySessionId(sessionId));
        stats.put("pending", stagedRepository.countBySessionIdAndStatus(sessionId, IngestedTransactionStatus.PENDING));
        stats.put("classified", stagedRepository.countBySessionIdAndStatus(sessionId, IngestedTransactionStatus.CLASSIFIED));
        stats.put("applied", stagedRepository.countBySessionIdAndStatus(sessionId, IngestedTransactionStatus.APPLIED));
        stats.put("discarded", stagedRepository.countBySessionIdAndStatus(sessionId, IngestedTransactionStatus.DISCARDED));
        stats.put("duplicate", stagedRepository.countBySessionIdAndStatus(sessionId, IngestedTransactionStatus.DUPLICATE));
        stats.put("errors", stagedRepository.countBySessionIdAndParseErrorIsNotNull(sessionId));
        return stats;
    }

    static String extractRuleKeyword(String description) {
        String s = description.trim();
        s = s.replaceAll("\\bREF[A-Za-z]*:\\s*\\S+\\b", "").trim();
        s = s.replaceAll("\\b[\\d.,]{4,}\\b", "").trim();
        s = s.replaceAll("\\s{2,}", " ").trim();
        if (s.length() > 50) {
            int idx = s.lastIndexOf(' ', 50);
            if (idx > 0) s = s.substring(0, idx);
        }
        return s.isBlank() ? description.trim() : s;
    }

    private static TransactionType parseTransactionType(String raw) {
        if (raw == null) return TransactionType.BANK_TRANSFER;
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "CARD_TRANSACTION" -> TransactionType.CARD_TRANSACTION;
            case "STOCK_PURCHASE" -> TransactionType.STOCK_PURCHASE;
            default -> TransactionType.BANK_TRANSFER;
        };
    }

    private static String fileHash(Path path) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] content = Files.readAllBytes(path);
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public record StagingResult(long sessionId, int totalRows, int pending, int errors, int duplicates, int classified) {}

    public record ApplyResult(long sessionId, int applied, int skipped, int remainingPending, int discarded) {}
}
