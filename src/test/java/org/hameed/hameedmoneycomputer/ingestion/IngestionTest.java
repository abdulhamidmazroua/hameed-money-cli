package org.hameed.hameedmoneycomputer.ingestion;

import org.hameed.hameedmoneycomputer.ingestion.SourceFormatConfig.ColumnMapping;
import org.hameed.hameedmoneycomputer.ledger.AccountType;
import org.hameed.hameedmoneycomputer.ledger.AccountView;
import org.hameed.hameedmoneycomputer.support.IntegrationTest;
import org.hameed.hameedmoneycomputer.support.LedgerFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Characterization tests for the current ingestion flow: parse → classify → edit → apply, plus dedupe and rules. */
@Import(LedgerFixture.class)
class IngestionTest extends IntegrationTest {

    private static final String SOURCE = "TEST_BANK";

    @Autowired LedgerFixture ledger;
    @Autowired SourceService sources;
    @Autowired StagingService staging;

    @TempDir Path dir;

    AccountView bank;
    AccountView groceries;
    AccountView salary;

    @BeforeEach
    void setUp() {
        bank = ledger.assetWithOpening("Ingest Bank", "USD", "0");
        groceries = ledger.account("Ingest Groceries", AccountType.EXPENSE, "USD");
        salary = ledger.account("Ingest Salary", AccountType.INCOME, "USD");

        SourceFormatConfig format = new SourceFormatConfig(",", true, 0,
                List.of(new ColumnMapping(0, "date", "Date"),
                        new ColumnMapping(1, "description", "Description"),
                        new ColumnMapping(2, "amount", "Amount")),
                List.of("yyyy-MM-dd"), "signed");
        sources.addSource("Test Bank", SOURCE, format, bank.id());
    }

    private String csv(String name, String... rows) throws Exception {
        Path file = dir.resolve(name);
        Files.writeString(file, "Date,Description,Amount\n" + String.join("\n", rows) + "\n");
        return file.toString();
    }

    // Row indexes count the header line, so the first data row is 1 (README examples using --row 0 hit the header).

    /** Assigning an account only sets an override; the row must also be marked CLASSIFIED to be applied. */
    private void assign(long sessionId, long row, AccountView account) {
        staging.editRow(sessionId, row, "account", account.name());
        staging.editRow(sessionId, row, "status", "CLASSIFIED");
    }

    @Test
    void assigningAnAccountAloneLeavesTheRowUnapplied() throws Exception {
        // Known UX gap: `ingest edit --field account` doesn't make a pending row applicable.
        var result = staging.parse(SOURCE, csv("s1.csv", "2025-11-02,SUPERMARKET 1234,-150.25"));
        staging.editRow(result.sessionId(), 1L, "account", groceries.name());

        assertEquals(0, staging.apply(result.sessionId()).applied());
    }

    private BigDecimal bankBalance() {
        return ledger.balance(bank);
    }

    @Test
    void parseStagesRowsAndFlagsErrors() throws Exception {
        var result = staging.parse(SOURCE, csv("s1.csv",
                "2025-11-02,SUPERMARKET 1234,-150.25",
                "2025-11-03,SALARY NOV,5000",
                "not-a-date,BROKEN ROW,10"));

        assertEquals(3, result.totalRows());
        assertEquals(1, result.errors());
        assertEquals(0, result.duplicates());
        assertEquals(0, result.classified());   // no rules yet, no LLM configured
    }

    @Test
    void applyPostsClassifiedRowsAgainstTheAnchoredAccount() throws Exception {
        var result = staging.parse(SOURCE, csv("s1.csv",
                "2025-11-02,SUPERMARKET 1234,-150.25",
                "2025-11-03,SALARY NOV,5000"));
        assign(result.sessionId(), 1L, groceries);
        assign(result.sessionId(), 2L, salary);

        var applied = staging.apply(result.sessionId());

        assertEquals(2, applied.applied());
        assertEquals(0, new BigDecimal("4849.75").compareTo(bankBalance()));
        assertEquals(0, new BigDecimal("150.25").compareTo(ledger.balance(groceries)));
    }

    @Test
    void reimportingTheSameFileProducesOnlyDuplicates() throws Exception {
        String file = csv("s1.csv", "2025-11-02,SUPERMARKET 1234,-150.25");
        var first = staging.parse(SOURCE, file);
        assign(first.sessionId(), 1L, groceries);
        staging.apply(first.sessionId());

        var second = staging.parse(SOURCE, file);

        assertEquals(1, second.duplicates());
        assertEquals(0, new BigDecimal("-150.25").compareTo(bankBalance()));
    }

    @Test
    void identicalSameDayRowsCollapseIntoOne() throws Exception {
        // Known gap: the dedupe key is source|date|description|amount, so two genuine identical purchases
        // on the same day are treated as one. Fixed by the new ingestion design (reference / occurrence index).
        var result = staging.parse(SOURCE, csv("s1.csv",
                "2025-11-02,COFFEE SHOP,-5",
                "2025-11-02,COFFEE SHOP,-5"));
        assign(result.sessionId(), 1L, groceries);
        assign(result.sessionId(), 2L, groceries);

        var applied = staging.apply(result.sessionId());

        assertEquals(1, applied.applied());
        assertEquals(0, new BigDecimal("-5").compareTo(bankBalance()));
    }

    @Test
    void applyLearnsARuleThatClassifiesTheNextImport() throws Exception {
        var first = staging.parse(SOURCE, csv("s1.csv", "2025-11-02,SUPERMARKET 1234,-150.25"));
        assign(first.sessionId(), 1L, groceries);
        staging.apply(first.sessionId());

        var next = staging.parse(SOURCE, csv("s2.csv", "2025-12-02,SUPERMARKET 9876,-80"));

        assertEquals(1, next.classified());
        var row = staging.getStagedRows(next.sessionId(), IngestedTransactionStatus.CLASSIFIED).get(0);
        assertEquals(groceries.id(), row.getSuggestedAccount().getId().longValue());
    }
}
