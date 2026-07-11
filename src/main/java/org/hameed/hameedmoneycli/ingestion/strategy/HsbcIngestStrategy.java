package org.hameed.hameedmoneycli.ingestion.strategy;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hameed.hameedmoneycli.enums.SourceSystemCode;
import org.hameed.hameedmoneycli.enums.TransactionType;
import org.hameed.hameedmoneycli.ingestion.IngestionRulePatternFactory;
import org.hameed.hameedmoneycli.ingestion.IngestionSupport;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.IngestionRule;
import org.hameed.hameedmoneycli.model.entity.SourceSystem;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.hameed.hameedmoneycli.repository.AccountRepository;
import org.hameed.hameedmoneycli.repository.IngestionRuleRepository;
import org.hameed.hameedmoneycli.repository.TransactionRepository;
import org.hameed.hameedmoneycli.util.DateUtil;
import org.hameed.hameedmoneycli.util.IngestionStrategy;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * HSBC Egypt mobile export: {@code Date,Description,"Amount"}. The non-anchored leg is chosen by
 * {@link IngestionRule} regex patterns (higher {@code priority} wins). For a positive CSV amount the
 * rule account is {@code fromAccount}; for a negative amount it is {@code toAccount}; the
 * {@link SourceSystem#getAnchoredAccount()} is always the other leg.
 * <p>
 * When no rule matches, the user can create a new rule interactively (target account + keyword or regex).
 */
@Component
@RequiredArgsConstructor
public class HsbcIngestStrategy implements IngestionStrategy {

    private static final String SKIP_SENTINEL = "__SKIP__";
    private static final DateTimeFormatter HSBC_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final IngestionRuleRepository ingestionRuleRepository;
    private final ComponentFlow.Builder componentFlowBuilder;

    @Override
    public SourceSystemCode supportedSource() {
        return SourceSystemCode.HSBC_APP;
    }

    @Override
    public List<Transaction> ingest(String filePath, SourceSystem sourceSystem, CommandContext ctx) throws IOException {
        Account anchored = accountRepository.findById(sourceSystem.getAnchoredAccount().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Anchored account not found for source system " + sourceSystem.getCode()));

        List<IngestionRule> rules = ingestionRuleRepository.findAllOrderedForMatching();
        if (rules.isEmpty()) {
            ctx.outputWriter().println(
                    "HSBC ingest: no ingestion_rule rows yet — you will be prompted to create rules for each new description.");
        }

        List<Transaction> result = new ArrayList<>();
        int skippedDuplicates = 0;
        int skippedBadRows = 0;
        int skippedUserSkip = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (Reader reader = Files.newBufferedReader(Path.of(filePath));
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                if (record.size() < 3) {
                    skippedBadRows++;
                    continue;
                }
                String dateRaw = record.get(0);
                String descriptionRaw = record.get(1);
                String amountRaw = record.get(2);

                try {
                    Long when = DateUtil.parseDateStringToMillis(dateRaw.trim(), HSBC_DATE);
                    BigDecimal signed = IngestionSupport.parseSignedAmount(amountRaw);
                    BigDecimal magnitude = IngestionSupport.absAmount(signed);

                    String externalRef = IngestionSupport.externalRefId(
                            sourceSystem.getCode(),
                            dateRaw,
                            descriptionRaw,
                            amountRaw);

                    if (transactionRepository.existsByExternalRefId(externalRef)) {
                        skippedDuplicates++;
                        continue;
                    }

                    Optional<Account> counterparty = resolveCounterparty(descriptionRaw, rules);
                    if (counterparty.isEmpty()) {
                        counterparty = promptCreateRule(descriptionRaw, ctx);
                        if (counterparty.isEmpty()) {
                            skippedUserSkip++;
                            continue;
                        }
                        rules = ingestionRuleRepository.findAllOrderedForMatching();
                    }

                    Account cp = counterparty.get();
                    if (anchored.getAsset() == null || cp.getAsset() == null) {
                        throw new IllegalStateException(
                                "Ingestion requires leaf accounts with an asset on both legs; check anchored account and rule target for: "
                                        + descriptionRaw);
                    }

                    String description = truncate(descriptionRaw);
                    LedgerLegs legs = ledgerLegs(signed, anchored, cp);
                    TransactionType txType = inferTransactionType(descriptionRaw);

                    Transaction tx = Transaction.builder()
                            .description(description)
                            .type(txType)
                            .transactionDate(when)
                            .fromAccount(legs.from())
                            .toAccount(legs.to())
                            .fromAmount(magnitude)
                            .toAmount(magnitude)
                            .feeAmount(BigDecimal.ZERO)
                            .externalRefId(externalRef)
                            .sourceSystem(sourceSystem)
                            .isSystemAdjustment(false)
                            .metadata(metadataFor(descriptionRaw, amountRaw))
                            .build();

                    result.add(tx);
                } catch (DateTimeParseException | IllegalArgumentException ex) {
                    skippedBadRows++;
                }
            }
        }

        ctx.outputWriter().printf(
                "HSBC ingest: %d new transaction(s); %d duplicate(s); %d bad row(s); %d row(s) skipped by you.%n",
                result.size(),
                skippedDuplicates,
                skippedBadRows,
                skippedUserSkip);

        return result;
    }

    /**
     * Positive amount: counterparty {@literal ->} anchored (money in). Negative: anchored {@literal ->} counterparty.
     */
    private static LedgerLegs ledgerLegs(BigDecimal signedAmount, Account anchored, Account counterparty) {
        if (signedAmount.signum() > 0) {
            return new LedgerLegs(counterparty, anchored);
        }
        return new LedgerLegs(anchored, counterparty);
    }

    private Optional<Account> resolveCounterparty(String description, List<IngestionRule> rules) {
        for (IngestionRule rule : rules) {
            try {
                Pattern p = Pattern.compile(rule.getMatchPattern(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                if (p.matcher(description).find()) {
                    return Optional.of(rule.getTargetAccount());
                }
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException(
                        "Invalid match_pattern for ingestion_rule id=" + rule.getId() + ": " + rule.getMatchPattern(),
                        e);
            }
        }
        return Optional.empty();
    }

    /**
     * Prompts for counterparty account and match pattern, persists a new {@link IngestionRule}, returns the account.
     */
    private Optional<Account> promptCreateRule(String descriptionRaw, CommandContext ctx) {
        ctx.outputWriter().println();
        ctx.outputWriter().println("--- No ingestion rule matched ---");
        ctx.outputWriter().println(descriptionRaw);
        ctx.outputWriter().println();

        List<SelectItem> items = new ArrayList<>();
        accountRepository.findAll().stream()
                .filter(a -> a.getAsset() != null)
                .sorted(Comparator.comparing(Account::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(a -> items.add(SelectItem.of(
                        a.getName() + " (" + a.getMasterType() + ", " + a.getAsset().getSymbol() + ", id=" + a.getId() + ")",
                        a.getId().toString())));
        items.add(SelectItem.of("Skip this row (no transaction, no new rule)", SKIP_SENTINEL));

        ComponentFlow.ComponentFlowResult pickAccount = componentFlowBuilder.clone().reset()
                .withSingleItemSelector("accountId")
                .name("Counterparty account for new rule (or Skip): ")
                .selectItems(items)
                .and()
                .build()
                .run();

        String accountChoice = pickAccount.getContext().get("accountId", String.class);
        if (accountChoice == null || SKIP_SENTINEL.equals(accountChoice)) {
            return Optional.empty();
        }

        long accountId = Long.parseLong(accountChoice);
        Account target = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Account not found: " + accountId));

        String defaultKeyword = IngestionRulePatternFactory.suggestDefaultKeywordForPrompt(descriptionRaw);

        ComponentFlow.ComponentFlowResult keywordResult = componentFlowBuilder.clone().reset()
                .withStringInput("keyword")
                .name("Keyword to match (contains, case-insensitive), or a full Java regex starting with (? or ^. Empty = auto from description.")
                .defaultValue(defaultKeyword)
                .and()
                .build()
                .run();

        String keywordRaw = keywordResult.getContext().get("keyword", String.class);
        String matchPattern;
        try {
            matchPattern = IngestionRulePatternFactory.toMatchPattern(keywordRaw, descriptionRaw);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex: " + e.getDescription(), e);
        }

        int priority = ingestionRuleRepository.findMaxPriority() + 1;
        IngestionRule rule = new IngestionRule();
        rule.setMatchPattern(matchPattern);
        rule.setTargetAccount(target);
        rule.setPriority(priority);
        ingestionRuleRepository.save(rule);

        ctx.outputWriter().printf("Saved ingestion_rule id=%d priority=%d pattern=%s -> %s%n",
                rule.getId(), priority, matchPattern, target.getName());
        ctx.outputWriter().println();

        return Optional.of(target);
    }

    private static TransactionType inferTransactionType(String rawDescription) {
        return rawDescription.toUpperCase(Locale.ROOT).contains("CARD TRANSACTION")
                ? TransactionType.CARD_TRANSACTION
                : TransactionType.BANK_TRANSFER;
    }

    private Map<String, String> metadataFor(String originalDescription, String amountRaw) {
        Map<String, String> metadata = new HashMap<>();
        String upper = originalDescription.toUpperCase(Locale.ROOT);
        String medium = null;
        if (upper.contains("ATM")) {
            medium = "ATM";
        } else if (upper.contains("SYSTEM GENERATED")) {
            medium = "INSTAPAY";
        }
        metadata.put("amount", amountRaw);
        metadata.put("medium", medium);
        metadata.put("description", originalDescription);
        return metadata;
    }

    private static String truncate(String description) {
        if (description.length() <= MAX_DESCRIPTION_LENGTH) {
            return description;
        }
        return description.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    private record LedgerLegs(Account from, Account to) {
    }
}
