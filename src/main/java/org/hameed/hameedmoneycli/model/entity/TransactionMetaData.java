package org.hameed.hameedmoneycli.model.entity;

public record TransactionMetaData(
        String merchantName,
        String merchantCode,
        String merchantCategory,
        String amount,
        String transactionMedium, // the system that initiated the transaction -- INSTAPAY, POS, ATM, BM_ONLINE, etc.
        String from_bank_account,
        String to_bank_account,
        String original_record_str
) {
}
