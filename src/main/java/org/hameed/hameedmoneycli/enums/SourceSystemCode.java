package org.hameed.hameedmoneycli.enums;

public enum SourceSystemCode {
    MANUAL_ENTRY, HSBC_APP, BANQUE_MISR_APP, THNDR_APP;

    public static SourceSystemCode fromString(String code) {
        for (SourceSystemCode sourceSystemCode : SourceSystemCode.values()) {
            if (sourceSystemCode.name().equalsIgnoreCase(code)) {
                return sourceSystemCode;
            }
        }
        throw new IllegalArgumentException("No enum constant for code: " + code);
    }
}
