package org.hameed.hameedmoneycli.enums;

public enum AccountType {
    ASSET, LIABILITY, INCOME, EXPENSE,
    /** Counterpart accounts for opening balance / reconcile adjustments — excluded from income vs expense reports. */
    SYSTEM;

    public boolean isInternal() {
        return switch (this) {
            case ASSET, LIABILITY -> true;
            case INCOME, EXPENSE, SYSTEM -> false;
        };
    }

    /** Use for cash-flow / P&L style reports (salary, spend). {@link #SYSTEM} is omitted. */
    public boolean isIncomeOrExpense() {
        return this == INCOME || this == EXPENSE;
    }

    public static AccountType fromString(String type) {
        for (AccountType accountType : AccountType.values()) {
            if (accountType.name().equalsIgnoreCase(type)) {
                return accountType;
            }
        }
        throw new IllegalArgumentException("No enum constant for type: " + type);
    }
}
