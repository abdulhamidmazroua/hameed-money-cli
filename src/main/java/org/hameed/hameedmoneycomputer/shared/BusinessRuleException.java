package org.hameed.hameedmoneycomputer.shared;

/** Input is valid, but the operation isn't allowed in the current state of the data or setup. */
public class BusinessRuleException extends HmcException {

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
