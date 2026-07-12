package org.hameed.hameedmoneycli.classifier;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.config.HmcConfig;
import org.hameed.hameedmoneycli.enums.IngestedTransactionStatus;
import org.hameed.hameedmoneycli.constants.PromptConstants;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.IngestedStagedTransaction;
import org.hameed.hameedmoneycli.proxy.LlmProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LLMClassifier implements Classifier {

    private static final Logger log = LoggerFactory.getLogger(LLMClassifier.class);

    private final HmcConfig hmcConfig;
    private final LlmProxy llmProxy;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "llm";
    }

    public void bulkClassify(List<IngestedStagedTransaction> rows, List<Account> candidateAccounts) {
        HmcConfig.LlmConfig llm = hmcConfig.getLlmConfig();
        if (llm == null || llm.provider() == null) return;

        String accountList = candidateAccounts.stream()
                .map(a -> "- " + a.getName() + " (" + a.getMasterType() + ", " + a.getAsset().getSymbol() + ")")
                .collect(Collectors.joining("\n"));

        String txnList = rows.stream()
                .map(r -> "Row " + r.getRowIndex() + ": \"" + r.getRawDescription() + "\" | Amount: " + r.getParsedAmount())
                .collect(Collectors.joining("\n"));

        String systemPrompt = llm.classifyPrompt() != null ? llm.classifyPrompt()
                : PromptConstants.LLM_CLASSIFY_SYSTEM_PROMPT_DEFAULT;

        String prompt = systemPrompt + "\n\nAvailable accounts:\n" + accountList
                + "\n\nTransactions to classify:\n" + txnList
                + "\n\nRespond with a JSON array where each element has: {\"rowIndex\": <int>, \"accountName\": \"...\", \"transactionType\": \"BANK_TRANSFER|CARD_TRANSACTION\", \"reasoning\": \"...\"}";

        String response;
        try {
            response = llmProxy.call(prompt, llm);
        } catch (Exception e) {
            log.warn("Bulk LLM classification call failed: {}", e.getMessage());
            return;
        }

        if (response == null || response.isBlank()) return;

        var results = parseBulkResponse(response, candidateAccounts);
        for (var result : results) {
            rows.stream()
                    .filter(r -> r.getRowIndex() == result.rowIndex())
                    .findFirst()
                    .ifPresent(row -> applyClassification(row, result));
        }
    }

    private void applyClassification(IngestedStagedTransaction row, ClassificationResult result) {
        row.setClassifier(name());
        row.setSuggestedAccount(result.account());
        row.setSuggestedTxType(result.transactionType());
        row.setConfidence(result.confidence());
        row.setLlmReasoning(result.reasoning());
        row.setStatus(IngestedTransactionStatus.CLASSIFIED);
    }

    private record ClassificationResult(int rowIndex, Account account, String transactionType, BigDecimal confidence, String reasoning) {}

    private List<ClassificationResult> parseBulkResponse(String response, List<Account> accounts) {
        try {
            String json = response.trim();
            if (json.startsWith("```json")) json = json.substring(7);
            if (json.startsWith("```")) json = json.substring(3);
            if (json.endsWith("```")) json = json.substring(0, json.length() - 3);
            json = json.trim();

            var arr = objectMapper.readTree(json);
            if (!arr.isArray()) return List.of();

            List<ClassificationResult> results = new java.util.ArrayList<>();
            for (var elem : arr) {
                int ri = elem.path("rowIndex").asInt(-1);
                if (ri < 0) continue;
                String accountName = elem.path("accountName").asText();
                if (accountName.isBlank()) continue;
                String txType = elem.path("transactionType").asText();
                String reasoning = elem.path("reasoning").asText();
                String finalAccountName = accountName;
                Account match = accounts.stream()
                        .filter(a -> a.getName().equalsIgnoreCase(finalAccountName)
                                || a.getName().contains(finalAccountName)
                                || finalAccountName.contains(a.getName()))
                        .findFirst().orElse(null);
                if (match == null) continue;
                results.add(new ClassificationResult(ri, match, txType, new BigDecimal("0.85"), reasoning));
            }
            return results;
        } catch (Exception e) {
            log.warn("Failed to parse bulk LLM response: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<Classification> classify(IngestedStagedTransaction row, List<Account> candidateAccounts) {
        HmcConfig.LlmConfig llm = hmcConfig.getLlmConfig();
        if (llm == null || llm.provider() == null) {
            return Optional.empty();
        }

        String prompt = buildPrompt(row, candidateAccounts, llm);
        String response;
        try {
            response = llmProxy.call(prompt, llm);
        } catch (Exception e) {
            log.warn("LLM classification call failed: {}", e.getMessage());
            return Optional.empty();
        }

        if (response == null || response.isBlank()) {
            return Optional.empty();
        }

        return parseResponse(response, candidateAccounts, row);
    }

    private String buildPrompt(IngestedStagedTransaction row, List<Account> accounts, HmcConfig.LlmConfig llm) {
        String systemPrompt = llm.classifyPrompt() != null ? llm.classifyPrompt()
                : PromptConstants.LLM_CLASSIFY_SYSTEM_PROMPT_DEFAULT;

        String accountList = accounts.stream()
                .map(a -> "- " + a.getName() + " (" + a.getMasterType() + ", " + a.getAsset().getSymbol() + ")")
                .collect(Collectors.joining("\n"));

        return systemPrompt + "\n\nAvailable accounts:\n" + accountList
                + "\n\nTransaction:\nDescription: " + row.getRawDescription()
                + "\nAmount: " + row.getParsedAmount()
                + "\n\nRespond with JSON only.";
    }

    private Optional<Classification> parseResponse(String response, List<Account> accounts, IngestedStagedTransaction row) {
        try {
            String json = response.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            int braceStart = json.indexOf('{');
            int braceEnd = json.lastIndexOf('}');
            if (braceStart >= 0 && braceEnd > braceStart) {
                json = json.substring(braceStart, braceEnd + 1);
            }

            var node = objectMapper.readTree(json);

            String accountName = node.path("accountName").asText();
            String txType = node.path("transactionType").asText(null);
            String reasoning = node.path("reasoning").asText(null);

            if (accountName == null || accountName.isBlank()) {
                return Optional.empty();
            }

            String finalAccountName = accountName;
            Optional<Account> match = accounts.stream()
                    .filter(a -> a.getName().equalsIgnoreCase(finalAccountName)
                            || a.getName().contains(finalAccountName)
                            || finalAccountName.contains(a.getName()))
                    .findFirst();

            if (match.isEmpty()) {
                match = accounts.stream()
                        .filter(a -> a.getAsset() != null
                                && a.getAsset().getSymbol().equalsIgnoreCase(row.getRawDescription()))
                        .findFirst();
            }

            return match.map(account -> new Classification(
                    account,
                    txType,
                    new BigDecimal("0.85"),
                    reasoning != null ? reasoning : "LLM classification"
            ));
        } catch (Exception e) {
            log.warn("Failed to parse LLM response: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
