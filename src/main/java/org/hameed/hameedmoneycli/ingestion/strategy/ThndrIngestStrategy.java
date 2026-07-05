package org.hameed.hameedmoneycli.ingestion.strategy;

import org.hameed.hameedmoneycli.enums.SourceSystemCode;
import org.hameed.hameedmoneycli.model.entity.SourceSystem;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.hameed.hameedmoneycli.util.IngestionStrategy;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class ThndrIngestStrategy implements IngestionStrategy {

    @Override
    public SourceSystemCode supportedSource() {
        return SourceSystemCode.THNDR_APP;
    }

    @Override
    public List<Transaction> ingest(String filePath, SourceSystem sourceSystem, CommandContext ctx) throws IOException {
        ctx.outputWriter().println("Thndr ingestion is not implemented yet.");
        return Collections.emptyList();
    }
}
