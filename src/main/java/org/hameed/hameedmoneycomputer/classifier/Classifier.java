package org.hameed.hameedmoneycomputer.classifier;

import org.hameed.hameedmoneycomputer.model.entity.Account;
import org.hameed.hameedmoneycomputer.model.entity.IngestedStagedTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface Classifier {

    String name();

    Optional<Classification> classify(IngestedStagedTransaction row, List<Account> candidateAccounts);

    record Classification(
            Account account,
            String transactionType,
            BigDecimal confidence,
            String reasoning
    ) {
        public Classification {
            if (confidence == null) confidence = BigDecimal.ONE;
        }

        private static final BigDecimal ONE = BigDecimal.ONE;
    }
}
