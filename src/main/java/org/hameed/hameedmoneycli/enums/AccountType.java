package org.hameed.hameedmoneycli.enums;

public enum AccountType {
    ASSET, LIABILITY, INCOME, EXPENSE;

    public boolean isInternal() {
        return switch (this) {
            case ASSET, LIABILITY -> true;
            case INCOME, EXPENSE -> false;
        };
    }
}
