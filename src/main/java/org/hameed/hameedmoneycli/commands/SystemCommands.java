package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.dto.RuleCreateDto;
import org.hameed.hameedmoneycli.service.BackupService;
import org.hameed.hameedmoneycli.service.IngestionRuleService;
import org.hameed.hameedmoneycli.service.IngestionService;
import org.hameed.hameedmoneycli.service.SystemAdjustmentService;
import org.hameed.hameedmoneycli.util.CommandsUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.hameed.hameedmoneycli.util.CommandsUtil.*;

@Configuration
@RequiredArgsConstructor
public class SystemCommands {

    private final IngestionService ingestionService;
    private final IngestionRuleService ingestionRuleService;
    private final SystemAdjustmentService systemAdjustmentService;
    private final BackupService backupService;

    @Bean
    public Command ingestTransactions() {
        return Command.builder()
                .name("ingest")
                .description("Import transactions from a CSV file")
                .help("Import transactions from a CSV file. Usage: `ingest HSBC_APP /path/to/transactions.csv` or `ingest --source HSBC_APP --file-path /path/to/transactions.csv`")
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .options(
                        CommandOption.with()
                                .shortName('f')
                                .longName("file-path")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('s')
                                .longName("source")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .execute(ctx -> {
                    String source = argOrOption(ctx, 0, 's', "source");
                    if (source == null) throw new IllegalArgumentException(required("source"));
                    String filePath = argOrOption(ctx, 1, 'f', "file-path");
                    if (filePath == null) throw new IllegalArgumentException(required("file-path"));
                    try {
                        ingestionService.ingestTransactions(source, filePath, ctx);
                    } catch (Exception e) {
                        throw new IllegalStateException("Ingestion failed: " + e.getMessage(), e);
                    }
                });
    }

    @Bean
    public Command addRule() {
        return Command.builder()
                .name("rule add")
                .description("Add a transaction ingestion rule")
                .help("Add a transaction ingestion rule. Usage: `rule add \"regex\" 5` or `rule add --pattern \"regex\" --target 5`")
                .options(CommandOption.with()
                                .shortName('p')
                                .longName("pattern")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName("target")
                                .required(false)
                                .type(String.class)
                                .build())
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String match = argOrOption(ctx, 0, 'p', "pattern");
                    if (match == null) throw new IllegalArgumentException(required("pattern"));
                    String target = argOrOption(ctx, 1, 't', "target");
                    if (target == null) throw new IllegalArgumentException(required("target"));

                    ingestionRuleService.addRule(new RuleCreateDto(
                            match,
                            Long.valueOf(target)
                    ));
                });
    }

    @Bean
    public Command hmcInit() {
        return Command.builder()
                .name("hmc init")
                .description("Post an opening balance to an account")
                .help("Post an opening balance to an existing account. Usage: `hmc init \"EGP:HSBC Current Account\" --balance 50000` or `hmc init --account \"EGP:HSBC Current Account\" --balance 50000`")
                .options(
                        CommandOption.with()
                                .shortName('a')
                                .longName("account")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('i')
                                .longName("account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('b')
                                .longName("balance")
                                .required(true)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String name = argOrOption(ctx, 0, 'a', "account");
                    String idStr = getOptionOrDefault(ctx, 'i', "account-id", null);
                    String balance = getOptionOrError(ctx, 'b', "balance", required("balance"));

                    if (idStr != null) {
                        systemAdjustmentService.initAccount(Long.valueOf(idStr), new BigDecimal(balance));
                    } else if (name != null) {
                        systemAdjustmentService.initAccount(name, new BigDecimal(balance));
                    } else {
                        throw new IllegalArgumentException("Either --account <name>, --account-id <id>, or a positional account name is required.");
                    }

                    ctx.outputWriter().println("Opening balance of " + balance + " posted.");
                });
    }

    @Bean
    public Command hmcReconcile() {
        return Command.builder()
                .name("hmc reconcile")
                .description("Reconcile an account to its actual balance")
                .help("Reconcile an account's computed balance to its actual balance via an adjustment transaction. Usage: `hmc reconcile \"EGP:HSBC Current Account\" --actual 49990` or `hmc reconcile --account \"EGP:HSBC Current Account\" --actual 49990`")
                .options(
                        CommandOption.with()
                                .shortName('a')
                                .longName("account")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('i')
                                .longName("account-id")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('c')
                                .longName("actual")
                                .required(true)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String name = argOrOption(ctx, 0, 'a', "account");
                    String idStr = getOptionOrDefault(ctx, 'i', "account-id", null);
                    String actual = getOptionOrError(ctx, 'c', "actual", required("actual"));

                    if (idStr != null) {
                        systemAdjustmentService.reconcileAccount(Long.valueOf(idStr), new BigDecimal(actual));
                    } else if (name != null) {
                        systemAdjustmentService.reconcileAccount(name, new BigDecimal(actual));
                    } else {
                        throw new IllegalArgumentException("Either --account <name>, --account-id <id>, or a positional account name is required.");
                    }

                    ctx.outputWriter().println("Reconciled to actual balance " + actual);
                });
    }

    @Bean
    public Command dbBackup() {
        return Command.builder()
                .name("hmc db backup")
                .description("Backup the database via pg_dump")
                .help("Backup the database using pg_dump. Usage: `hmc db backup` or `hmc db backup --output ~/hmc/backups`")
                .options(
                        CommandOption.with()
                                .longName("output")
                                .shortName('o')
                                .required(false)
                                .type(String.class)
                                .defaultValue("~/hmc/backups")
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    try {
                        String outputDir = getOptionOrDefault(ctx, 'o', "output", "~/hmc/backups");
                        outputDir = outputDir.replaceFirst("^~", System.getProperty("user.home"));

                        Path backupFile = backupService.backup(outputDir);
                        ctx.outputWriter().println("Backup saved: " + backupFile.toAbsolutePath());
                    } catch (Exception e) {
                        throw new RuntimeException("Backup failed: " + e.getMessage(), e);
                    }
                });
    }

    @Bean
    public Command showInfo() {
        return Command.builder()
                .name("info")
                .description("Show the financial data pipeline guide")
                .help("Display the financial data pipeline guide showing all available commands and how they fit together. Usage: `info`")
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    ctx.outputWriter().print(CommandsUtil.guidelines());
                });
    }
}
