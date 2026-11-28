package org.hameed.hameedmoneycomputer.ledger;

import org.hameed.hameedmoneycomputer.shared.ValidationException;

public enum TransactionType {
    CARD_TRANSACTION, BANK_TRANSFER, STOCK_PURCHASE, SYSTEM_ADJUSTMENT;

    public static TransactionType fromString(String type) {
        for (TransactionType transactionType : TransactionType.values()) {
            if (transactionType.name().equalsIgnoreCase(type)) {
                return transactionType;
            }
        }
        throw new ValidationException("Unknown transaction type '" + type + "'. Valid: " + java.util.Arrays.toString(values()));
    }
}