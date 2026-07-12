package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.classifier.SourceDetector;
import org.hameed.hameedmoneycli.model.entity.SourceFormatConfig;
import org.hameed.hameedmoneycli.model.entity.SourceSystem;
import org.hameed.hameedmoneycli.service.SourceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;

import java.util.*;

import static org.hameed.hameedmoneycli.constants.CommandConstants.*;
import static org.hameed.hameedmoneycli.util.CommandsUtil.*;

@Configuration
@RequiredArgsConstructor
public class SourceCommands {

    private final SourceService sourceService;
    private final SourceDetector sourceDetector;

    @Bean
    public Command sourceList() {
        return Command.builder()
                .name("source list")
                .description(SOURCE_LIST_COMMAND_DESCRIPTION)
                .help(SOURCE_LIST_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    List<SourceSystem> sources = sourceService.listSources();
                    if (sources.isEmpty()) {
                        ctx.outputWriter().println("No source systems defined. Use " + ANSI_CYAN + "source add" + ANSI_RESET + " to create one.");
                        return;
                    }

                    ctx.outputWriter().printf("%-4s %-20s %-15s %-8s %s%n", "ID", "Name", "Code", "Anchored", "Created");
                    ctx.outputWriter().println("-".repeat(80));

                    for (SourceSystem s : sources) {
                        String anchoredName = s.getAnchoredAccount() != null
                                ? s.getAnchoredAccount().getName()
                                : "(none)";
                        ctx.outputWriter().printf("%-4d %-20s %-15s %-8s %s%n",
                                s.getId(), s.getName(), s.getCode(),
                                anchoredName,
                                new Date(s.getCreatedAt()).toString().substring(4, 16));
                    }
                });
    }

    @Bean
    public Command sourceShow() {
        return Command.builder()
                .name("source show")
                .description(SOURCE_SHOW_COMMAND_DESCRIPTION)
                .help(SOURCE_SHOW_COMMAND_HELP)
                .options(
                        CommandOption.with().shortName('c').longName(SOURCE_CODE_ARG).required(false).type(String.class).build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String code = argOrOption(ctx, 0, 'c', SOURCE_CODE_ARG);
                    if (code == null) throw new IllegalArgumentException(SOURCE_SHOW_CODE_ARG_ERROR);

                    SourceSystem s = sourceService.getSourceByCode(code);
                    ctx.outputWriter().println("ID:     " + s.getId());
                    ctx.outputWriter().println("Name:   " + s.getName());
                    ctx.outputWriter().println("Code:   " + s.getCode());
                    ctx.outputWriter().println("Anchored account: "
                            + (s.getAnchoredAccount() != null ? s.getAnchoredAccount().getName() + " (ID: " + s.getAnchoredAccount().getId() + ")" : "(none)"));

                    if (s.getFormatConfig() != null) {
                        SourceFormatConfig cfg = s.getFormatConfig();
                        ctx.outputWriter().println("Format config:");
                        ctx.outputWriter().println("  Delimiter:      '" + cfg.delimiter() + "'");
                        ctx.outputWriter().println("  Has header:     " + cfg.hasHeader());
                        ctx.outputWriter().println("  Skip lines:     " + cfg.skipLines());
                        ctx.outputWriter().println("  Amount pattern: " + cfg.amountPattern());
                        ctx.outputWriter().println("  Date formats:   " + String.join(", ", cfg.dateFormats()));
                        ctx.outputWriter().println("  Columns:");
                        for (SourceFormatConfig.ColumnMapping col : cfg.columns()) {
                            ctx.outputWriter().printf("    [%d] %s (%s)%n", col.index(), col.name(), col.field());
                        }
                    } else {
                        ctx.outputWriter().println("Format config: (none)");
                    }

                    ctx.outputWriter().println("Created: " + new Date(s.getCreatedAt()));
                });
    }

    @Bean
    public Command sourceAdd() {
        return Command.builder()
                .name("source add")
                .description(SOURCE_ADD_COMMAND_DESCRIPTION)
                .help(SOURCE_ADD_COMMAND_HELP)
                .options(
                        CommandOption.with().shortName('n').longName(SOURCE_NAME_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('c').longName(SOURCE_CODE_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('f').longName(SOURCE_FILE_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('a').longName(SOURCE_ACCOUNT_ARG).required(false).type(String.class).build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String name = argOrOption(ctx, 0, 'n', SOURCE_NAME_ARG);
                    if (name == null) throw new IllegalArgumentException(SOURCE_ADD_NAME_ARG_ERROR);

                    String code = argOrOption(ctx, 1, 'c', SOURCE_CODE_ARG);
                    if (code == null) throw new IllegalArgumentException(SOURCE_ADD_CODE_ARG_ERROR);

                    if (sourceService.existsByCode(code)) {
                        throw new IllegalArgumentException(String.format(SOURCE_ADD_EXISTS_ERROR, code));
                    }

                    String filePath = argOrOption(ctx, 2, 'f', SOURCE_FILE_ARG);

                    Long accountId = null;
                    String accountStr = getOptionOrDefault(ctx, 'a', SOURCE_ACCOUNT_ARG, null);
                    if (accountStr != null && !accountStr.isBlank()) {
                        accountId = Long.parseLong(accountStr);
                    }

                    if (filePath != null) {
                        SourceDetector.DetectedFormat detected;
                        try {
                            detected = sourceDetector.detect(filePath);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to detect format: " + e.getMessage(), e);
                        }

                        if (detected.error() != null) {
                            throw new RuntimeException("Format detection failed: " + detected.error());
                        }

                        SourceFormatConfig config = detected.config();
                        ctx.outputWriter().println("Detected format config:");
                        ctx.outputWriter().println("  Delimiter:      '" + config.delimiter() + "'");
                        ctx.outputWriter().println("  Has header:     " + config.hasHeader());
                        ctx.outputWriter().println("  Skip lines:     " + config.skipLines());
                        ctx.outputWriter().println("  Amount pattern: " + config.amountPattern());
                        ctx.outputWriter().println("  Date formats:   " + String.join(", ", config.dateFormats()));
                        ctx.outputWriter().println("  Columns:");
                        for (SourceFormatConfig.ColumnMapping col : config.columns()) {
                            ctx.outputWriter().printf("    [%d] %s (%s)%n", col.index(), col.name(), col.field());
                        }

                        SourceSystem saved = sourceService.addSource(name, code, config, accountId);
                        ctx.outputWriter().println(ANSI_CYAN + "Source '" + saved.getName() + "' (" + saved.getCode() + ") created. ID: " + saved.getId() + ANSI_RESET);
                    } else {
                        SourceSystem saved = sourceService.addSource(name, code, null, accountId);
                        ctx.outputWriter().println(ANSI_CYAN + "Source '" + saved.getName() + "' (" + saved.getCode() + ") created with no format config. ID: " + saved.getId() + ANSI_RESET);
                        ctx.outputWriter().println("Add a format config later with " + ANSI_CYAN + "source update-format" + ANSI_RESET + ".");
                    }
                });
    }

    @Bean
    public Command sourceRemove() {
        return Command.builder()
                .name("source remove")
                .description(SOURCE_REMOVE_COMMAND_DESCRIPTION)
                .help(SOURCE_REMOVE_COMMAND_HELP)
                .options(
                        CommandOption.with().shortName('c').longName(SOURCE_CODE_ARG).required(false).type(String.class).build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String code = argOrOption(ctx, 0, 'c', SOURCE_CODE_ARG);
                    if (code == null) throw new IllegalArgumentException(SOURCE_REMOVE_CODE_ARG_ERROR);

                    sourceService.removeSource(code);
                    ctx.outputWriter().println("Source '" + code + "' removed.");
                });
    }

    @Bean
    public Command sourceUpdateAccount() {
        return Command.builder()
                .name("source update-account")
                .description(SOURCE_UPDATE_ACCOUNT_COMMAND_DESCRIPTION)
                .help(SOURCE_UPDATE_ACCOUNT_COMMAND_HELP)
                .options(
                        CommandOption.with().shortName('c').longName(SOURCE_CODE_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('a').longName(SOURCE_ACCOUNT_ARG).required(false).type(String.class).build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String code = argOrOption(ctx, 0, 'c', SOURCE_CODE_ARG);
                    if (code == null) throw new IllegalArgumentException(SOURCE_UPDATE_ACCOUNT_CODE_ARG_ERROR);

                    String accountStr = argOrOption(ctx, 1, 'a', SOURCE_ACCOUNT_ARG);
                    if (accountStr == null) throw new IllegalArgumentException(SOURCE_UPDATE_ACCOUNT_ACCOUNT_ARG_ERROR);

                    sourceService.updateAnchoredAccount(code, Long.parseLong(accountStr));
                    ctx.outputWriter().println("Source '" + code + "' account updated.");
                });
    }

    @Bean
    public Command sourceUpdateFormat() {
        return Command.builder()
                .name("source update-format")
                .description(SOURCE_UPDATE_FORMAT_COMMAND_DESCRIPTION)
                .help(SOURCE_UPDATE_FORMAT_COMMAND_HELP)
                .options(
                        CommandOption.with().shortName('c').longName(SOURCE_CODE_ARG).required(false).type(String.class).build(),
                        CommandOption.with().shortName('f').longName(SOURCE_FILE_ARG).required(false).type(String.class).build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String code = argOrOption(ctx, 0, 'c', SOURCE_CODE_ARG);
                    if (code == null) throw new IllegalArgumentException(SOURCE_UPDATE_FORMAT_CODE_ARG_ERROR);

                    String filePath = argOrOption(ctx, 1, 'f', SOURCE_FILE_ARG);
                    if (filePath == null) throw new IllegalArgumentException(SOURCE_UPDATE_FORMAT_FILE_ARG_ERROR);

                    SourceDetector.DetectedFormat detected;
                    try {
                        detected = sourceDetector.detect(filePath);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to detect format: " + e.getMessage(), e);
                    }

                    if (detected.error() != null) {
                        throw new RuntimeException("Format detection failed: " + detected.error());
                    }

                    sourceService.updateFormatConfig(code, detected.config());
                    ctx.outputWriter().println("Source '" + code + "' format config updated.");
                });
    }

    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_CYAN = "\033[36m";
}
