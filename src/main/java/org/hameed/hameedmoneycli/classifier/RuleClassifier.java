package org.hameed.hameedmoneycli.classifier;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.IngestedStagedTransaction;
import org.hameed.hameedmoneycli.model.entity.IngestionRule;
import org.hameed.hameedmoneycli.repository.IngestionRuleRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
@RequiredArgsConstructor
public class RuleClassifier implements Classifier {

    private final IngestionRuleRepository ingestionRuleRepository;

    @Override
    public String name() {
        return "rules";
    }

    @Override
    public Optional<Classification> classify(IngestedStagedTransaction row, List<Account> candidateAccounts) {
        List<IngestionRule> rules = ingestionRuleRepository.findAllOrderedForMatching();
        String description = row.getRawDescription();

        for (IngestionRule rule : rules) {
            try {
                Pattern p = Pattern.compile(rule.getMatchPattern(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                if (p.matcher(description).find()) {
                    return Optional.of(new Classification(
                            rule.getTargetAccount(),
                            inferTransactionType(description),
                            BigDecimal.ONE,
                            "Matched rule id=" + rule.getId() + " pattern=" + rule.getMatchPattern()
                    ));
                }
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException(
                        "Invalid match_pattern for ingestion_rule id=" + rule.getId() + ": " + rule.getMatchPattern(), e);
            }
        }

        return Optional.empty();
    }

    private static String inferTransactionType(String description) {
        return description.toUpperCase(java.util.Locale.ROOT).contains("CARD TRANSACTION")
                ? "CARD_TRANSACTION"
                : "BANK_TRANSFER";
    }
}
