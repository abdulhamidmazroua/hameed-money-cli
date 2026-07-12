package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.dto.RuleCreateDto;
import org.hameed.hameedmoneycli.service.AccountService;
import org.hameed.hameedmoneycli.service.BackupService;
import org.hameed.hameedmoneycli.service.IngestionRuleService;
import org.hameed.hameedmoneycli.service.SystemAdjustmentService;
import org.hameed.hameedmoneycli.util.CommandsUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.hameed.hameedmoneycli.util.CommandsUtil.*;
import static org.hameed.hameedmoneycli.constants.CommandConstants.*;

@Configuration
@RequiredArgsConstructor
public class SystemCommands {

    private final AccountService accountService;
    private final IngestionRuleService ingestionRuleService;
    private final SystemAdjustmentService systemAdjustmentService;
    private final BackupService backupService;

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
                    if (match == null) throw new IllegalArgumentException(RULE_ADD_PATTERN_ARG_ERROR);
                    String target = argOrOption(ctx, 1, 't', TARGET_ARG);
                    if (target == null) throw new IllegalArgumentException(RULE_ADD_TARGET_ARG_ERROR);

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
                    if (balance == null) throw new IllegalArgumentException(HMC_INIT_BALANCE_ARG_ERROR);

                    Long accountId;
                    if (idStr != null) {
                        accountId = Long.valueOf(idStr);
                    } else if (name != null) {
                        accountId = accountService.getAccountByName(name).getId();
                    } else {
                        throw new IllegalArgumentException(HMC_INIT_ACCOUNT_NOT_FOUND);
                    }

                    systemAdjustmentService.openAccountBalance(accountId, new BigDecimal(balance));
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
                    if (actual == null) throw new IllegalArgumentException(HMC_RECONCILE_ACTUAL_ARG_ERROR);

                    Long accountId;
                    if (idStr != null) {
                        accountId = Long.valueOf(idStr);
                    } else if (name != null) {
                        accountId = accountService.getAccountByName(name).getId();
                    } else {
                        throw new IllegalArgumentException(HMC_RECONCILE_ACCOUNT_NOT_FOUND);
                    }

                    systemAdjustmentService.adjustBalance(accountId, new BigDecimal(actual));
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
                    ctx.outputWriter().print(CommandsUtil.manual());
                });
    }
}
