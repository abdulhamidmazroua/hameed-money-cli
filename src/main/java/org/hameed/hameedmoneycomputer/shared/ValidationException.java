package org.hameed.hameedmoneycomputer.shared;

/** Input is malformed or out of range (bad date, unknown enum value, missing field). */
public class ValidationException extends HmcException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
