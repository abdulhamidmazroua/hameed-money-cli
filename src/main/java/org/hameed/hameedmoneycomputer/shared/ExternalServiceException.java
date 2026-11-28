package org.hameed.hameedmoneycomputer.shared;

/** An external source (market data, LLM, extraction tool) failed or returned nothing usable. */
public class ExternalServiceException extends HmcException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
