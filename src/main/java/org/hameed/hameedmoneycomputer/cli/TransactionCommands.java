package org.hameed.hameedmoneycomputer.cli;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.hameed.hameedmoneycomputer.ledger.AccountService;
import org.hameed.hameedmoneycomputer.ledger.TransactionCreateDto;
import org.hameed.hameedmoneycomputer.ledger.TransactionFilter;
import org.hameed.hameedmoneycomputer.ledger.TransactionService;
import org.hameed.hameedmoneycomputer.ledger.TransactionType;
import org.hameed.hameedmoneycomputer.ledger.TransactionView;
import org.hameed.hameedmoneycomputer.shared.DateUtil;
import org.hameed.hameedmoneycomputer.shared.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hameed.hameedmoneycomputer.cli.CommandsUtil.*;
import static org.hameed.hameedmoneycomputer.cli.CommandConstants.*;

@Configuration
@RequiredArgsConstructor
public class TransactionCommands {

    private static final String[] CSV_HEADER = {"description", "type", "transaction date", "from account",
            "from amount", "to account", "to amount", "fee amount", "source system", "record creation date"};
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final ComponentFlow.Builder componentFlowBuilder;

    @Value("${hmc.report.output-dir}")
    private String reportOutputDir;

    @Bean
    public Command addTransaction() {
        return Command.builder()
                .name("transaction add")
                .description(TRANSACTION_ADD_COMMAND_DESCRIPTION)
                .help(TRANSACTION_ADD_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('a')
                                .longName(AMOUNT_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('f')
                                .longName(FROM_AMOUNT_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName(TO_AMOUNT_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('d')
                                .longName(DATE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('D')
                                .longName(DESCRIPTION_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('F')
                                .longName(FROM_ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('T')
                                .longName(TO_ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('N')
                                .longName(FROM_ACCOUNT_NAME_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('M')
                                .longName(TO_ACCOUNT_NAME_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName(FEE_AMOUNT_ARG)
                                .required(false)
                                .type(String.class)
                                .build())
                .execute(ctx -> {
                    String dateArg = getOptionOrDefault(ctx, 'd', DATE_ARG, null);
                    LocalDate date = dateArg == null ? DateUtil.today() : DateUtil.parseUserDate(dateArg);

                    var args = ctx.parsedInput().arguments();

                    // Resolve from-account: positional (auto-detect ID/name), or --from-account-id/--from-account-name
                    String fromAccountId;
                    if (!args.isEmpty()) {
                        String raw = args.get(0).value();
                        fromAccountId = raw.matches("\\d+") ? raw : String.valueOf(accountService.getAccountByName(raw).id());
                    } else {
                        fromAccountId = getOptionOrDefault(ctx, 'F', FROM_ACCOUNT_ID_ARG, null);
                        String fromAccountName = getOptionOrDefault(ctx, 'N', FROM_ACCOUNT_NAME_ARG, null);
                        if (fromAccountId == null && fromAccountName != null) {
                            fromAccountId = String.valueOf(accountService.getAccountByName(fromAccountName).id());
                        }
                    }

                    // Resolve to-account: positional (auto-detect ID/name), or --to-account-id/--to-account-name
                    String toAccountId;
                    if (args.size() > 1) {
                        String raw = args.get(1).value();
                        toAccountId = raw.matches("\\d+") ? raw : String.valueOf(accountService.getAccountByName(raw).id());
                    } else {
                        toAccountId = getOptionOrDefault(ctx, 'T', TO_ACCOUNT_ID_ARG, null);
                        String toAccountName = getOptionOrDefault(ctx, 'M', TO_ACCOUNT_NAME_ARG, null);
                        if (toAccountId == null && toAccountName != null) {
                            toAccountId = String.valueOf(accountService.getAccountByName(toAccountName).id());
                        }
                    }

                    if (fromAccountId == null) {
                        throw new ValidationException(TRANSACTION_ADD_FROM_ACCOUNT_ARG_ERROR);
                    }
                    if (toAccountId == null) {
                        throw new ValidationException(TRANSACTION_ADD_TO_ACCOUNT_ARG_ERROR);
                    }
                    String description = getOptionOrDefault(ctx, 'D', DESCRIPTION_ARG, null);
                    String feeAmount = getOptionOrDefault(ctx, 'e', FEE_AMOUNT_ARG, "0");

                    // Resolve amount: positional arg 2, or --amount, or --from-amount/--to-amount
                    String fromAmount;
                    String toAmount;
                    if (args.size() > 2) {
                        String posAmount = args.get(2).value();
                        fromAmount = posAmount;
                        toAmount = posAmount;
                    } else {
                        String amount = getOptionOrDefault(ctx, 'a', AMOUNT_ARG, null);
                        if (amount != null && !amount.isBlank()) {
                            fromAmount = amount;
                            toAmount = amount;
                        } else {
                            fromAmount = getOptionOrError(ctx, 'f', FROM_AMOUNT_ARG, TRANSACTION_ADD_AMOUNT_ARG_ERROR);
                            toAmount = getOptionOrError(ctx, 't', TO_AMOUNT_ARG, TRANSACTION_ADD_TO_AMOUNT_ARG_ERROR);
                        }
                    }

                    ComponentFlow.ComponentFlowResult transactionTypeResult = componentFlowBuilder.clone().reset()
                            .withSingleItemSelector("transactionType")
                            .name("Transaction Type: ")
                            .selectItems(List.of(TransactionType.values()).stream()
                                    .map(transactionType -> SelectItem.of(transactionType.toString(), transactionType.toString()))
                                    .toList()).and().build().run();

                    TransactionCreateDto transactionCreateDto = new TransactionCreateDto(
                            description != null ? description : "",
                            TransactionType.fromString(transactionTypeResult.getContext().get("transactionType", String.class)),
                            parseId(fromAccountId, FROM_ACCOUNT_ID_ARG),
                            parseId(toAccountId, TO_ACCOUNT_ID_ARG),
                            parseAmount(fromAmount, FROM_AMOUNT_ARG),
                            parseAmount(toAmount, TO_AMOUNT_ARG),
                            date,
                            "MANUAL_ENTRY",
                            parseAmount(feeAmount, FEE_AMOUNT_ARG)
                    );

                    transactionService.createTransaction(transactionCreateDto);
                });
    }

    @Bean
    public Command listTransactions() {
        return Command.builder()
                .name("transaction list")
                .description(TRANSACTION_LIST_COMMAND_DESCRIPTION)
                .help(TRANSACTION_LIST_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('T')
                                .longName(TRANSACTION_TYPE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('f')
                                .longName(FROM_ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName(TO_ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName(START_DATE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName(END_DATE_ARG)
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String transactionType = getOptionOrDefault(ctx, 'T', TRANSACTION_TYPE_ARG, null);
                    String fromAccountId = getOptionOrDefault(ctx, 'f', FROM_ACCOUNT_ID_ARG, null);
                    String toAccountId = getOptionOrDefault(ctx, 't', TO_ACCOUNT_ID_ARG, null);
                    String startDate = getOptionOrDefault(ctx, 's', START_DATE_ARG, null);
                    String endDate = getOptionOrDefault(ctx, 'e', END_DATE_ARG, null);

                    TransactionFilter transactionFilter = new TransactionFilter(
                            transactionType == null ? null : TransactionType.fromString(transactionType),
                            optionalId(fromAccountId, FROM_ACCOUNT_ID_ARG),
                            optionalId(toAccountId, TO_ACCOUNT_ID_ARG),
                            optionalDate(startDate),
                            optionalDate(endDate)
                    );

                    printTransactions(ctx, transactionService.getTransactions(transactionFilter));
                });
    }

    @Bean
    public Command findTransactions() {
        return Command.builder()
                .name("transaction find")
                .description(TRANSACTION_FIND_COMMAND_DESCRIPTION)
                .help(TRANSACTION_FIND_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('T')
                                .longName(TRANSACTION_TYPE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('f')
                                .longName(FROM_ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName(TO_ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName(START_DATE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName(END_DATE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('a')
                                .longName(ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .longName(MIN_AMOUNT_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .longName(MAX_AMOUNT_ARG)
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String keyword = argOrOption(ctx, 0, 'k', KEYWORD_ARG);
                    String transactionType = getOptionOrDefault(ctx, 'T', TRANSACTION_TYPE_ARG, null);
                    String fromAccountId = getOptionOrDefault(ctx, 'f', FROM_ACCOUNT_ID_ARG, null);
                    String toAccountId = getOptionOrDefault(ctx, 't', TO_ACCOUNT_ID_ARG, null);
                    String startDate = getOptionOrDefault(ctx, 's', START_DATE_ARG, null);
                    String endDate = getOptionOrDefault(ctx, 'e', END_DATE_ARG, null);
                    String accountId = getOptionOrDefault(ctx, 'a', ACCOUNT_ID_ARG, null);
                    String minAmount = getOptionOrDefault(ctx, (char) 0, MIN_AMOUNT_ARG, null);
                    String maxAmount = getOptionOrDefault(ctx, (char) 0, MAX_AMOUNT_ARG, null);

                    TransactionFilter transactionFilter = new TransactionFilter(
                            transactionType == null ? null : TransactionType.fromString(transactionType),
                            optionalId(fromAccountId, FROM_ACCOUNT_ID_ARG),
                            optionalId(toAccountId, TO_ACCOUNT_ID_ARG),
                            optionalDate(startDate),
                            optionalDate(endDate),
                            keyword,
                            minAmount != null && !minAmount.isBlank() ? parseAmount(minAmount, MIN_AMOUNT_ARG) : null,
                            maxAmount != null && !maxAmount.isBlank() ? parseAmount(maxAmount, MAX_AMOUNT_ARG) : null,
                            optionalId(accountId, ACCOUNT_ID_ARG)
                    );

                    printTransactions(ctx, transactionService.getTransactions(transactionFilter));
                });
    }

    @Bean
    public Command generateTransactionReport() {
        return Command.builder()
                .name("transaction report")
                .description(TRANSACTION_REPORT_COMMAND_DESCRIPTION)
                .help(TRANSACTION_REPORT_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('T')
                                .longName(TRANSACTION_TYPE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('f')
                                .longName(FROM_ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName(TO_ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName(START_DATE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName(END_DATE_ARG)
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String transactionType = getOptionOrDefault(ctx, 'T', TRANSACTION_TYPE_ARG, null);
                    String fromAccountId = getOptionOrDefault(ctx, 'f', FROM_ACCOUNT_ID_ARG, null);
                    String toAccountId = getOptionOrDefault(ctx, 't', TO_ACCOUNT_ID_ARG, null);
                    String startDate = getOptionOrDefault(ctx, 's', START_DATE_ARG, null);
                    String endDate = getOptionOrDefault(ctx, 'e', END_DATE_ARG, null);

                    TransactionFilter transactionFilter = new TransactionFilter(
                            transactionType == null ? null : TransactionType.fromString(transactionType),
                            optionalId(fromAccountId, FROM_ACCOUNT_ID_ARG),
                            optionalId(toAccountId, TO_ACCOUNT_ID_ARG),
                            optionalDate(startDate),
                            optionalDate(endDate));
                    List<TransactionView> transactions = transactionService.getTransactions(transactionFilter);
                    Path path = writeCsv(transactions);
                    ctx.outputWriter().println("Wrote " + transactions.size() + " transaction(s) to " + path);
                });
    }

    private void printTransactions(CommandContext ctx, List<TransactionView> transactions) {
        ctx.outputWriter().printf("%-12s | %-50s | %-18s | %-18s | %-20s | %-12s%n", "Type", "From Account -> To Account", "From Amount", "To Amount", "Date", "Fee");
        transactions.forEach(
                transaction -> {
                    String fromAccountName = transaction.from().accountName() + " (ID: " + transaction.from().accountId() + ")";
                    String toAccountName = transaction.to().accountName() + " (ID: " + transaction.to().accountId() + ")";
                    String accountPair = fromAccountName + " -> " + toAccountName;
                    ctx.outputWriter().printf("%-12s | %-50s | %-18s | %-18s | %-20s | %-12s%n",
                            transaction.type(),
                            accountPair.length() > 50 ? accountPair.substring(0, 47) + "..." : accountPair,
                            transaction.from().amount() + " " + symbol(transaction.from()),
                            transaction.to().amount() + " " + symbol(transaction.to()),
                            DateUtil.formatDateTime(transaction.date()),
                            transaction.fee() + " " + symbol(transaction.from())
                    );
                }
        );
    }

    /** Writes the transactions as CSV to a new timestamped file under {@code hmc.report.output-dir}. */
    private Path writeCsv(List<TransactionView> transactions) {
        Path path = Path.of(reportOutputDir)
                .resolve("transaction_report-" + LocalDateTime.now(DateUtil.zone()).format(FILE_STAMP) + ".csv");
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(CSV_HEADER).setRecordSeparator('\n').build();
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path);
                 CSVPrinter csv = new CSVPrinter(writer, format)) {
                for (TransactionView t : transactions) {
                    csv.printRecord(
                            t.description(),
                            t.type().name(),
                            DateUtil.formatDateTime(t.date()),
                            t.from().accountName() + " " + symbol(t.from()),
                            t.from().amount().toPlainString(),
                            t.to().accountName() + " " + symbol(t.to()),
                            t.to().amount().toPlainString(),
                            t.fee().toPlainString(),
                            t.source() == null ? "" : t.source(),
                            t.createdAt() == null ? "" : DateUtil.formatDateTime(t.createdAt()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write transaction report to " + path + ": " + e.getMessage(), e);
        }
        return path;
    }

    private static String symbol(TransactionView.Leg leg) {
        return leg.assetSymbol() == null ? "\u2014" : leg.assetSymbol();
    }

    private static Long optionalId(String raw, String label) {
        return raw == null || raw.isBlank() ? null : parseId(raw, label);
    }

    private static LocalDate optionalDate(String raw) {
        return raw == null || raw.isBlank() ? null : DateUtil.parseUserDate(raw);
    }
}
