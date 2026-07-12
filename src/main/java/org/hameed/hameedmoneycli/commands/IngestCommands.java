package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.IngestedTransactionStatus;
import org.hameed.hameedmoneycli.model.entity.*;
import org.hameed.hameedmoneycli.service.StagingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;

import java.util.*;

import static org.hameed.hameedmoneycli.constants.CommandConstants.*;
import static org.hameed.hameedmoneycli.util.CommandsUtil.*;

@Configuration
@RequiredArgsConstructor
public class IngestCommands {

    private final StagingService stagingService;

    private static final String SOURCE_ARG = "source";
    private static final String FILE_PATH_ARG = "file-path";
    private static final String SESSION_ARG = "session";
    private static final String ROW_ARG = "row";
    private static final String STATUS_ARG = "status";
    private static final String UNMATCHED_ARG = "unmatched";
    private static final String FIELD_ARG = "field";
    private static final String VALUE_ARG = "value";

    @Bean
    public Command ingestParse() {
        return Command.builder()
                .name("ingest parse")
                .description(INGEST_PARSE_COMMAND_DESCRIPTION)
                .help(INGEST_PARSE_COMMAND_HELP)
                .options(
                        CommandOption.with().shortName('s').longName(SOURCE_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('f').longName(FILE_PATH_ARG).required(false).type(String.class).build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String source = argOrOption(ctx, 0, 's', SOURCE_ARG);
                    if (source == null) throw new IllegalArgumentException(INGEST_PARSE_SOURCE_ARG_ERROR);
                    String filePath = argOrOption(ctx, 1, 'f', FILE_PATH_ARG);
                    if (filePath == null) throw new IllegalArgumentException(INGEST_PARSE_FILE_ARG_ERROR);

                    try {
                        StagingService.StagingResult result = stagingService.parse(source, filePath);
                        ctx.outputWriter().printf("Staged %d row(s) from %s (session %d): %d classified, %d pending, %d errors, %d duplicates%n",
                                result.totalRows(), source, result.sessionId(), result.classified(), result.pending(), result.errors(), result.duplicates());
                        ctx.outputWriter().println("Run " + ANSI_CYAN + "ingest review --session " + result.sessionId() + ANSI_RESET + " to view.");
                        ctx.outputWriter().println("Run " + ANSI_CYAN + "ingest apply --session " + result.sessionId() + ANSI_RESET + " to commit to ledger.");
                    } catch (java.io.IOException e) {
                        throw new RuntimeException("Failed to parse file: " + e.getMessage(), e);
                    }
                });
    }

    @Bean
    public Command ingestSessions() {
        return Command.builder()
                .name("ingest sessions")
                .description(INGEST_SESSIONS_COMMAND_DESCRIPTION)
                .help(INGEST_SESSIONS_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    List<IngestionStagingSession> sessions = stagingService.listSessions();
                    if (sessions.isEmpty()) {
                        ctx.outputWriter().println("No staging sessions found.");
                        return;
                    }

                    ctx.outputWriter().printf("%-4s %-12s %-8s %-6s %-20s %s%n",
                            "ID", "Source", "Status", "Rows", "File", "Created");
                    ctx.outputWriter().println("-".repeat(80));

                    for (IngestionStagingSession s : sessions) {
                        Map<String, Long> stats = stagingService.getSessionStats(s.getId());
                        long pending = stats.getOrDefault("pending", 0L);
                        long errors = stats.getOrDefault("errors", 0L);
                        String statusFlag = pending > 0 ? " (" + pending + " pending)" : "";
                        String errorFlag = errors > 0 ? " [" + errors + " errors]" : "";
                        ctx.outputWriter().printf("%-4d %-12s %-8s %-6d %-20s %s%s%s%n",
                                s.getId(), s.getSourceCode(), s.getStatus(),
                                s.getTotalRows(), s.getFileName(),
                                new java.util.Date(s.getCreatedAt()).toString().substring(4, 16),
                                statusFlag, errorFlag);
                    }
                });
    }

    @Bean
    public Command ingestReview() {
        return Command.builder()
                .name("ingest review")
                .description(INGEST_REVIEW_COMMAND_DESCRIPTION)
                .help(INGEST_REVIEW_COMMAND_HELP)
                .options(
                        CommandOption.with().shortName('n').longName(SESSION_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('s').longName(STATUS_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('u').longName(UNMATCHED_ARG).required(false).type(Boolean.class).build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String sessionStr = argOrOption(ctx, 0, 'n', SESSION_ARG);
                    if (sessionStr == null) throw new IllegalArgumentException(INGEST_REVIEW_SESSION_ARG_ERROR);

                    Long sessionId = Long.parseLong(sessionStr);
                    String statusStr = argOrOption(ctx, 1, 's', STATUS_ARG, null);
                    boolean unmatchedOnly = Boolean.parseBoolean(argOrOption(ctx, 2, 'u', UNMATCHED_ARG, "false"));

                    IngestedTransactionStatus statusFilter = null;
                    if (statusStr != null) {
                        try {
                            statusFilter = IngestedTransactionStatus.valueOf(statusStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            ctx.outputWriter().println(String.format(INGEST_REVIEW_INVALID_STATUS, statusStr));
                            return;
                        }
                    }

                    IngestionStagingSession session = stagingService.getSession(sessionId);
                    List<IngestedStagedTransaction> rows;

                    if (unmatchedOnly) {
                        rows = stagingService.getStagedRows(sessionId, IngestedTransactionStatus.PENDING);
                        rows = rows.stream()
                                .filter(r -> r.getSuggestedAccount() == null && r.getOverrideAccount() == null)
                                .toList();
                    } else if (statusFilter != null) {
                        rows = stagingService.getStagedRows(sessionId, statusFilter);
                    } else {
                        rows = stagingService.getStagedRows(sessionId, null);
                    }

                    if (rows.isEmpty()) {
                        ctx.outputWriter().println(INGEST_REVIEW_NO_ROWS);
                        return;
                    }

                    ctx.outputWriter().printf("Session %d (%s — %s) — %d row(s)%n%n",
                            sessionId, session.getSourceCode(), session.getStatus(), rows.size());

                    ctx.outputWriter().printf("%-3s %-8s %-14s %-40s %-10s %-25s %-6s%n",
                            "#", "Status", "Date", "Description", "Amount", "Account", "Conf");
                    ctx.outputWriter().println("-".repeat(120));

                    java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");

                    for (IngestedStagedTransaction row : rows) {
                        String dateStr = row.getParsedDate() != null
                                ? org.hameed.hameedmoneycli.util.DateUtil.getDateStringFromMillis(row.getParsedDate(), dateFmt)
                                : ANSI_YELLOW + row.getRawDate() + ANSI_RESET;

                        String desc = row.getRawDescription().length() > 38
                                ? row.getRawDescription().substring(0, 35) + "..."
                                : row.getRawDescription();

                        String amountStr = row.getParsedAmount() != null
                                ? row.getParsedAmount().toPlainString()
                                : ANSI_YELLOW + row.getRawAmount() + ANSI_RESET;

                        Account acc = row.effectiveAccount();
                        String accountStr = acc != null ? acc.getName() : ANSI_YELLOW + "—" + ANSI_RESET;
                        if (accountStr.length() > 23) accountStr = accountStr.substring(0, 20) + "...";

                        String confStr = row.getConfidence() != null ? row.getConfidence().toPlainString() : "—";

                        String statusColor = switch (row.getStatus()) {
                            case PENDING, DUPLICATE, DISCARDED -> ANSI_YELLOW;
                            default -> "";
                        };

                        ctx.outputWriter().printf("%-3d %s%-8s%s %-14s %-40s %-10s %-25s %-6s%n",
                                row.getRowIndex(), statusColor, row.getStatus(), ANSI_RESET,
                                dateStr, desc, amountStr, accountStr, confStr);
                    }

                    if (unmatchedOnly || statusFilter == null) {
                        Map<String, Long> stats = stagingService.getSessionStats(sessionId);
                        ctx.outputWriter().println();
                        ctx.outputWriter().printf("Stats: %d total | %d classified | %d pending | %d applied | %d discarded | %d duplicates | %d errors%n",
                                stats.getOrDefault("total", 0L),
                                stats.getOrDefault("classified", 0L),
                                stats.getOrDefault("pending", 0L),
                                stats.getOrDefault("applied", 0L),
                                stats.getOrDefault("discarded", 0L),
                                stats.getOrDefault("duplicate", 0L),
                                stats.getOrDefault("errors", 0L));
                    }
                });
    }

    @Bean
    public Command ingestApply() {
        return Command.builder()
                .name("ingest apply")
                .description(INGEST_APPLY_COMMAND_DESCRIPTION)
                .help(INGEST_APPLY_COMMAND_HELP)
                .options(
                        CommandOption.with().shortName('n').longName(SESSION_ARG).required(false).type(String.class).build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String sessionStr = argOrOption(ctx, 0, 'n', SESSION_ARG);
                    if (sessionStr == null) throw new IllegalArgumentException(INGEST_APPLY_SESSION_ARG_ERROR);

                    Long sessionId = Long.parseLong(sessionStr);
                    StagingService.ApplyResult result = stagingService.apply(sessionId);

                    ctx.outputWriter().printf("Applied %d row(s) to ledger (session %d). %d skipped, %d remaining pending, %d discarded.%n",
                            result.applied(), result.sessionId(), result.skipped(),
                            result.remainingPending(), result.discarded());
                });
    }

    @Bean
    public Command ingestDiscard() {
        return Command.builder()
                .name("ingest discard")
                .description(INGEST_DISCARD_COMMAND_DESCRIPTION)
                .help(INGEST_DISCARD_COMMAND_HELP)
                .options(
                        CommandOption.with().shortName('n').longName(SESSION_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('r').longName(ROW_ARG).required(false).type(String.class).build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String sessionStr = argOrOption(ctx, 0, 'n', SESSION_ARG);
                    if (sessionStr == null) throw new IllegalArgumentException(INGEST_DISCARD_SESSION_ARG_ERROR);

                    Long sessionId = Long.parseLong(sessionStr);
                    String rowStr = argOrOption(ctx, 1, 'r', ROW_ARG, null);

                    if (rowStr != null) {
                        stagingService.discard(sessionId, Long.parseLong(rowStr));
                        ctx.outputWriter().println("Row " + rowStr + " discarded.");
                    } else {
                        stagingService.discard(sessionId, null);
                        ctx.outputWriter().println("Session " + sessionId + " cancelled.");
                    }
                });
    }

    @Bean
    public Command ingestEdit() {
        return Command.builder()
                .name("ingest edit")
                .description(INGEST_EDIT_COMMAND_DESCRIPTION)
                .help(INGEST_EDIT_COMMAND_HELP)
                .options(
                        CommandOption.with().shortName('n').longName(SESSION_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('r').longName(ROW_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('f').longName(FIELD_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('v').longName(VALUE_ARG).required(false).type(String.class).build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String sessionStr = argOrOption(ctx, 0, 'n', SESSION_ARG);
                    if (sessionStr == null) throw new IllegalArgumentException(INGEST_EDIT_SESSION_ARG_ERROR);
                    String rowStr = argOrOption(ctx, 1, 'r', ROW_ARG);
                    if (rowStr == null) throw new IllegalArgumentException(INGEST_EDIT_ROW_ARG_ERROR);
                    String field = argOrOption(ctx, 2, 'f', FIELD_ARG);
                    if (field == null) throw new IllegalArgumentException(INGEST_EDIT_FIELD_ARG_ERROR);
                    String value = argOrOption(ctx, 3, 'v', VALUE_ARG);
                    if (value == null) throw new IllegalArgumentException(INGEST_EDIT_VALUE_ARG_ERROR);

                    Long sessionId = Long.parseLong(sessionStr);
                    Integer rowIndex = Integer.parseInt(rowStr);

                    stagingService.editRow(sessionId, rowIndex, field, value);
                    ctx.outputWriter().printf("Row %d in session %d updated (%s = %s).%n", rowIndex, sessionId, field, value);
                    ctx.outputWriter().println("Run " + ANSI_CYAN + "ingest review --session " + sessionId + ANSI_RESET + " to view.");
                });
    }

    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_CYAN = "\033[36m";
    private static final String ANSI_YELLOW = "\033[33m";
}
