package org.hameed.hameedmoneycli.enums;

public enum TransactionType {
    CARD_TRANSACTION, BANK_TRANSFER, STOCK_PURCHASE, SYSTEM_ADJUSTMENT;

    public static TransactionType fromString(String type) {
        for (TransactionType transactionType : TransactionType.values()) {
            if (transactionType.name().equalsIgnoreCase(type)) {
                return transactionType;
            }
        }
        throw new IllegalArgumentException("No enum constant for type: " + type);
    }
}