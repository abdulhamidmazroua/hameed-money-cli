package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.TransactionType;
import org.hameed.hameedmoneycli.model.dto.TransactionCreateDto;
import org.hameed.hameedmoneycli.model.dto.TransactionFilter;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.hameed.hameedmoneycli.service.AccountService;
import org.hameed.hameedmoneycli.service.TransactionService;
import org.hameed.hameedmoneycli.util.DateUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hameed.hameedmoneycli.util.CommandsUtil.*;
import static org.hameed.hameedmoneycli.constants.CommandConstants.*;

@Configuration
@RequiredArgsConstructor
public class TransactionCommands {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final ComponentFlow.Builder componentFlowBuilder;

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
                    String date = getOptionOrDefault(ctx, 'd', DATE_ARG, LocalDate.now().format(DateTimeFormatter.ofPattern(DateUtil.DEFAULT_DATE_FORMAT)));

                    var args = ctx.parsedInput().arguments();

                    // Resolve from-account: positional (auto-detect ID/name), or --from-account-id/--from-account-name
                    String fromAccountId;
                    if (!args.isEmpty()) {
                        String raw = args.get(0).value();
                        fromAccountId = raw.matches("\\d+") ? raw : accountService.getAccountByName(raw).getId().toString();
                    } else {
                        fromAccountId = getOptionOrDefault(ctx, 'F', FROM_ACCOUNT_ID_ARG, null);
                        String fromAccountName = getOptionOrDefault(ctx, 'N', FROM_ACCOUNT_NAME_ARG, null);
                        if (fromAccountId == null && fromAccountName != null) {
                            fromAccountId = accountService.getAccountByName(fromAccountName).getId().toString();
                        }
                    }

                    // Resolve to-account: positional (auto-detect ID/name), or --to-account-id/--to-account-name
                    String toAccountId;
                    if (args.size() > 1) {
                        String raw = args.get(1).value();
                        toAccountId = raw.matches("\\d+") ? raw : accountService.getAccountByName(raw).getId().toString();
                    } else {
                        toAccountId = getOptionOrDefault(ctx, 'T', TO_ACCOUNT_ID_ARG, null);
                        String toAccountName = getOptionOrDefault(ctx, 'M', TO_ACCOUNT_NAME_ARG, null);
                        if (toAccountId == null && toAccountName != null) {
                            toAccountId = accountService.getAccountByName(toAccountName).getId().toString();
                        }
                    }

                    if (fromAccountId == null) {
                        throw new IllegalArgumentException(TRANSACTION_ADD_FROM_ACCOUNT_ARG_ERROR);
                    }
                    if (toAccountId == null) {
                        throw new IllegalArgumentException(TRANSACTION_ADD_TO_ACCOUNT_ARG_ERROR);
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
                            Long.valueOf(fromAccountId),
                            Long.valueOf(toAccountId),
                            new BigDecimal(fromAmount),
                            new BigDecimal(toAmount),
                            date,
                            "MANUAL_ENTRY",
                            new BigDecimal(feeAmount)
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
                            transactionType,
                            fromAccountId != null && !fromAccountId.isBlank() ? Long.valueOf(fromAccountId) : null,
                            toAccountId != null && !toAccountId.isBlank() ? Long.valueOf(toAccountId) : null,
                            startDate,
                            endDate
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
                            transactionType,
                            fromAccountId != null && !fromAccountId.isBlank() ? Long.valueOf(fromAccountId) : null,
                            toAccountId != null && !toAccountId.isBlank() ? Long.valueOf(toAccountId) : null,
                            startDate,
                            endDate,
                            keyword,
                            minAmount != null && !minAmount.isBlank() ? new BigDecimal(minAmount) : null,
                            maxAmount != null && !maxAmount.isBlank() ? new BigDecimal(maxAmount) : null,
                            accountId != null && !accountId.isBlank() ? Long.valueOf(accountId) : null
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
                            transactionType,
                            fromAccountId != null && !fromAccountId.isBlank() ? Long.valueOf(fromAccountId) : null,
                            toAccountId != null && !toAccountId.isBlank() ? Long.valueOf(toAccountId) : null,
                            startDate,
                            endDate);
                    try {
                        transactionService.generateTransactionReport(transactionFilter);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void printTransactions(CommandContext ctx, List<Transaction> transactions) {
        ctx.outputWriter().printf("%-12s | %-50s | %-18s | %-18s | %-20s | %-12s%n", "Type", "From Account -> To Account", "From Amount", "To Amount", "Date", "Fee");
        transactions.forEach(
                transaction -> {
                    String fromAccountName = transaction.getFromAccount().getName() + " (ID: " + transaction.getFromAccount().getId() + ")";
                    String toAccountName = transaction.getToAccount().getName() + " (ID: " + transaction.getToAccount().getId() + ")";
                    String accountPair = fromAccountName + " -> " + toAccountName;
                    ctx.outputWriter().printf("%-12s | %-50s | %-18s | %-18s | %-20s | %-12s%n",
                            transaction.getType(),
                            accountPair.length() > 50 ? accountPair.substring(0, 47) + "..." : accountPair,
                            transaction.getFromAmount() + " " + assetSymbol(transaction.getFromAccount()),
                            transaction.getToAmount() + " " + assetSymbol(transaction.getToAccount()),
                            DateUtil.getDateTimeStringFromMillis(transaction.getTransactionDate()),
                            transaction.getFeeAmount() + " " + assetSymbol(transaction.getFromAccount())
                    );
                }
        );
    }
}
