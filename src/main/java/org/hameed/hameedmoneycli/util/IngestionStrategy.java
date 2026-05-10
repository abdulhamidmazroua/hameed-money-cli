package org.hameed.hameedmoneycli.util;

import org.hameed.hameedmoneycli.enums.SourceSystemCode;
import org.hameed.hameedmoneycli.model.entity.SourceSystem;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.springframework.shell.core.command.CommandContext;

import java.io.IOException;
import java.util.List;

public interface IngestionStrategy {

    /**
     * Which {@link SourceSystemCode} this strategy handles (matches {@link SourceSystem#getCode()}).
     */
    SourceSystemCode supportedSource();

    List<Transaction> ingest(String filePath, SourceSystem sourceSystem, CommandContext ctx) throws IOException;

}
