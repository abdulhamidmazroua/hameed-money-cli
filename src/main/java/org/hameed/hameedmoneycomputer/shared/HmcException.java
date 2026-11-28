package org.hameed.hameedmoneycomputer.shared;

/**
 * Base class for expected, user-facing errors raised by the core. Clients map the subtype to their own
 * representation (CLI exit status, RPC error code) and show the message as-is.
 */
public abstract class HmcException extends RuntimeException {

    protected HmcException(String message) {
        super(message);
    }

    protected HmcException(String message, Throwable cause) {
        super(message, cause);
    }
}
