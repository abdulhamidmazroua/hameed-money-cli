package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.SourceSystemCode;
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
                .description("Record a transaction between two accounts")
                .help("Record a transaction between two accounts. Usage: `transaction add --from-amount 100 --to-amount 100 --fee-amount 2.2 --date 2024-01-01 --description \"Grocery shopping\" --from-account-id 1 --to-account-id 2` \n Note: use `--amount` instead of `--from-amount` and `--to-amount` if both sides match. \n Use `--from-account-name`/`--to-account-name` instead of IDs.")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('a')
                                .longName("amount")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('f')
                                .longName("from-amount")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName("to-amount")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('d')
                                .longName("date")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('D')
                                .longName("description")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('F')
                                .longName("from-account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('T')
                                .longName("to-account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('N')
                                .longName("from-account-name")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('M')
                                .longName("to-account-name")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName("fee-amount")
                                .required(false)
                                .type(String.class)
                                .build())
                .execute(ctx -> {
                    String usage = "Usage: `transaction add --from-amount 100 --to-amount 100 --fee-amount 2.2 --date 2024-01-01 --description \"Grocery shopping\" --from-account-id 1 --to-account-id 2` \n Note: you can also use `--amount` option instead of `--from-amount` and `--to-amount` if the amounts are the same for both sides of the transaction.";
                    String date = getOptionOrDefault(ctx, 'd', "date", LocalDate.now().format(DateTimeFormatter.ofPattern(DateUtil.DEFAULT_DATE_FORMAT)));
                    String fromAccountId = getOptionOrDefault(ctx, 'F', "from-account-id", null);
                    String toAccountId = getOptionOrDefault(ctx, 'T', "to-account-id", null);
                    String fromAccountName = getOptionOrDefault(ctx, 'N', "from-account-name", null);
                    String toAccountName = getOptionOrDefault(ctx, 'M', "to-account-name", null);

                    if (fromAccountId == null && fromAccountName != null) {
                        fromAccountId = accountService.getAccountByName(fromAccountName).getId().toString();
                    }
                    if (toAccountId == null && toAccountName != null) {
                        toAccountId = accountService.getAccountByName(toAccountName).getId().toString();
                    }
                    if (fromAccountId == null) {
                        throw new IllegalArgumentException("Either --from-account-id or --from-account-name is required.");
                    }
                    if (toAccountId == null) {
                        throw new IllegalArgumentException("Either --to-account-id or --to-account-name is required.");
                    }
                    String description = getOptionOrDefault(ctx, 'D', "description", null);
                    String feeAmount = getOptionOrDefault(ctx, 'e', "fee-amount", "0");

                    String amount = getOptionOrDefault(ctx, 'a', "amount", null);
                    String fromAmount;
                    String toAmount;

                    if (amount != null && !amount.isBlank()) {
                        fromAmount = amount;
                        toAmount = amount;
                    } else {
                        fromAmount = getOptionOrError(ctx, 'f', "from-amount", "<amount> or <from-amount> option is missing. \n" + usage);
                        toAmount = getOptionOrError(ctx, 't', "to-amount", "<amount> or <to-amount> option is missing. \n" + usage);
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
                            SourceSystemCode.MANUAL_ENTRY,
                            new BigDecimal(feeAmount)
                    );

                    transactionService.createTransaction(transactionCreateDto);
                });
    }

    @Bean
    public Command listTransactions() {
        return Command.builder()
                .name("transaction list")
                .description("List transactions with optional filters")
                .help("List transactions with optional filters. Usage: `transaction list --transaction-type CARD_PURCHASE --from-account-id 1 --to-account-id 2 --start-date 2024-01-01 --end-date 2024-12-31` \n All options are optional \u2014 omitting them lists all transactions.")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('T')
                                .longName("transaction-type")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('f')
                                .longName("from-account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName("to-account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName("start-date")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName("end-date")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String transactionType = getOptionOrDefault(ctx, 'T', "transaction-type", null);
                    String fromAccountId = getOptionOrDefault(ctx, 'f', "from-account-id", null);
                    String toAccountId = getOptionOrDefault(ctx, 't', "to-account-id", null);
                    String startDate = getOptionOrDefault(ctx, 's', "start-date", null);
                    String endDate = getOptionOrDefault(ctx, 'e', "end-date", null);

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
                .description("Search transactions by keyword, amount range, account, type, or date")
                .help("Search transactions by description keyword, amount range, account, type, or date. Usage: `transaction find uber` or `transaction find --min-amount 100 --max-amount 500 --start-date 2024-01-01` or `transaction find --account-id 5`")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('T')
                                .longName("transaction-type")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('f')
                                .longName("from-account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName("to-account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName("start-date")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName("end-date")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('a')
                                .longName("account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .longName("min-amount")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .longName("max-amount")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String keyword = argOrOption(ctx, 0, 'k', "keyword");
                    String transactionType = getOptionOrDefault(ctx, 'T', "transaction-type", null);
                    String fromAccountId = getOptionOrDefault(ctx, 'f', "from-account-id", null);
                    String toAccountId = getOptionOrDefault(ctx, 't', "to-account-id", null);
                    String startDate = getOptionOrDefault(ctx, 's', "start-date", null);
                    String endDate = getOptionOrDefault(ctx, 'e', "end-date", null);
                    String accountId = getOptionOrDefault(ctx, 'a', "account-id", null);
                    String minAmount = getOptionOrDefault(ctx, (char) 0, "min-amount", null);
                    String maxAmount = getOptionOrDefault(ctx, (char) 0, "max-amount", null);

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
                .description("Export transactions to a CSV report")
                .help("Export filtered transactions to a CSV file. Usage: `transaction report --transaction-type CARD_PURCHASE --from-account-id 1 --to-account-id 2 --start-date 2024-01-01 --end-date 2024-12-31` \n All options are optional \u2014 omitting them exports all transactions.")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('T')
                                .longName("transaction-type")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('f')
                                .longName("from-account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName("to-account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName("start-date")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('e')
                                .longName("end-date")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String transactionType = getOptionOrDefault(ctx, 'T', "transaction-type", null);
                    String fromAccountId = getOptionOrDefault(ctx, 'f', "from-account-id", null);
                    String toAccountId = getOptionOrDefault(ctx, 't', "to-account-id", null);
                    String startDate = getOptionOrDefault(ctx, 's', "start-date", null);
                    String endDate = getOptionOrDefault(ctx, 'e', "end-date", null);

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
                            transaction.getTransactionDate(),
                            transaction.getFeeAmount() + " " + assetSymbol(transaction.getFromAccount())
                    );
                }
        );
    }
}
