package org.hameed.hameedmoneycomputer.cli;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.ingestion.IngestionRuleService;
import org.hameed.hameedmoneycomputer.ingestion.RuleCreateDto;
import org.hameed.hameedmoneycomputer.ledger.AccountService;
import org.hameed.hameedmoneycomputer.ledger.SetupService;
import org.hameed.hameedmoneycomputer.shared.BackupService;
import org.hameed.hameedmoneycomputer.shared.ValidationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;

import java.nio.file.Path;

import static org.hameed.hameedmoneycomputer.cli.CommandsUtil.*;
import static org.hameed.hameedmoneycomputer.cli.CommandConstants.*;

@Configuration
@RequiredArgsConstructor
public class SystemCommands {

    private final AccountService accountService;
    private final IngestionRuleService ingestionRuleService;
    private final SetupService setupService;
    private final BackupService backupService;
    private final ComponentFlow.Builder componentFlowBuilder;

    @Bean
    public Command setup() {
        return Command.builder()
                .name("setup")
                .description(SETUP_COMMAND_DESCRIPTION)
                .help(SETUP_COMMAND_HELP)
                .options(CommandOption.with()
                        .shortName('c')
                        .longName(CURRENCY_ARG)
                        .required(false)
                        .type(String.class)
                        .build())
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String currency = argOrOption(ctx, 0, 'c', CURRENCY_ARG);
                    if (currency == null) {
                        currency = componentFlowBuilder.clone().reset()
                                .withStringInput("currency")
                                .name("Base currency (ISO 4217 code): ")
                                .and().build().run()
                                .getContext().get("currency", String.class);
                    }
                    SetupService.SetupResult result = setupService.setup(currency);
                    ctx.outputWriter().println("Base currency set to " + result.baseCurrency() + ".");
                    if (!result.createdAccounts().isEmpty()) {
                        ctx.outputWriter().println("Created " + result.createdAccounts().size() + " starter accounts: "
                                + String.join(", ", result.createdAccounts()));
                        ctx.outputWriter().println("Next: add your real accounts, e.g. `account init --name \"Wallet\" --asset "
                                + result.baseCurrency() + " --balance <amount>`.");
                    }
                });
    }

    @Bean
    public Command addRule() {
        return Command.builder()
                .name("rule add")
                .description(RULE_ADD_COMMAND_DESCRIPTION)
                .help(RULE_ADD_COMMAND_HELP)
                .options(CommandOption.with()
                                .shortName('p')
                                .longName(PATTERN_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('t')
                                .longName(TARGET_ARG)
                                .required(false)
                                .type(String.class)
                                .build())
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String match = argOrOption(ctx, 0, 'p', PATTERN_ARG);
                    if (match == null) throw new ValidationException(RULE_ADD_PATTERN_ARG_ERROR);
                    String target = argOrOption(ctx, 1, 't', TARGET_ARG);
                    if (target == null) throw new ValidationException(RULE_ADD_TARGET_ARG_ERROR);

                    ingestionRuleService.addRule(new RuleCreateDto(
                            match,
                            parseId(target, TARGET_ARG)
                    ));
                });
    }

    @Bean
    public Command hmcInit() {
        return Command.builder()
                .name("hmc init")
                .description(HMC_INIT_COMMAND_DESCRIPTION)
                .help(HMC_INIT_COMMAND_HELP)
                .options(
                        CommandOption.with()
                                .shortName('a')
                                .longName(ACCOUNT_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('i')
                                .longName(ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('b')
                                .longName(BALANCE_ARG)
                                .required(true)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String name = argOrOption(ctx, 0, 'a', ACCOUNT_ARG);
                    String idStr = getOptionOrDefault(ctx, 'i', ACCOUNT_ID_ARG, null);
                    String balance = argOrOption(ctx, 1, 'b', BALANCE_ARG);
                    if (balance == null) throw new ValidationException(HMC_INIT_BALANCE_ARG_ERROR);

                    long accountId = resolveAccountId(idStr, name, HMC_INIT_ACCOUNT_NOT_FOUND);
                    accountService.openBalance(accountId, parseAmount(balance, BALANCE_ARG));
                    ctx.outputWriter().println("Opening balance of " + balance + " posted.");
                });
    }

    @Bean
    public Command hmcReconcile() {
        return Command.builder()
                .name("hmc reconcile")
                .description(HMC_RECONCILE_COMMAND_DESCRIPTION)
                .help(HMC_RECONCILE_COMMAND_HELP)
                .options(
                        CommandOption.with()
                                .shortName('a')
                                .longName(ACCOUNT_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('i')
                                .longName(ACCOUNT_ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('c')
                                .longName(ACTUAL_ARG)
                                .required(true)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String name = argOrOption(ctx, 0, 'a', ACCOUNT_ARG);
                    String idStr = getOptionOrDefault(ctx, 'i', ACCOUNT_ID_ARG, null);
                    String actual = argOrOption(ctx, 1, 'c', ACTUAL_ARG);
                    if (actual == null) throw new ValidationException(HMC_RECONCILE_ACTUAL_ARG_ERROR);

                    long accountId = resolveAccountId(idStr, name, HMC_RECONCILE_ACCOUNT_NOT_FOUND);
                    accountService.reconcile(accountId, parseAmount(actual, ACTUAL_ARG));
                    ctx.outputWriter().println("Reconciled to actual balance " + actual);
                });
    }

    @Bean
    public Command dbBackup() {
        return Command.builder()
                .name("hmc db backup")
                .description(HMC_DB_BACKUP_COMMAND_DESCRIPTION)
                .help(HMC_DB_BACKUP_COMMAND_HELP)
                .options(
                        CommandOption.with()
                                .longName(OUTPUT_ARG)
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
                        String outputDir = getOptionOrDefault(ctx, 'o', OUTPUT_ARG, "~/hmc/backups");
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
                .description(INFO_COMMAND_DESCRIPTION)
                .help(INFO_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .execute(ctx -> {
                    CommandsUtil.pagedOutput(CommandsUtil.manual());
                });
    }

    private long resolveAccountId(String idStr, String name, String missingMessage) {
        if (idStr != null) {
            return parseId(idStr, ACCOUNT_ID_ARG);
        }
        if (name != null) {
            return accountService.getAccountByName(name).id();
        }
        throw new ValidationException(missingMessage);
    }
}
