package org.hameed.hameedmoneycomputer.shared;

/** A referenced record (by id, name, symbol or code) does not exist. */
public class NotFoundException extends HmcException {

    public NotFoundException(String message) {
        super(message);
    }
}
