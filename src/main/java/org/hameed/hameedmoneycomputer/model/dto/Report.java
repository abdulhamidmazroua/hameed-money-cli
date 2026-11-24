package org.hameed.hameedmoneycomputer.model.dto;

import java.io.PrintWriter;

public interface Report {
    void terminalPrint(PrintWriter out);

    default void export() {
        throw new UnsupportedOperationException("Export not yet supported");
    }
}
