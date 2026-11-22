package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.config.HmcConfig;
import org.hameed.hameedmoneycli.model.dto.ReportPeriod;
import org.hameed.hameedmoneycli.service.AccountService;
import org.hameed.hameedmoneycli.service.AuditService;
import org.hameed.hameedmoneycli.service.ReportService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;
import org.hameed.hameedmoneycli.util.DateUtil;

import java.time.LocalDate;
import java.util.List;

import static org.hameed.hameedmoneycli.util.CommandsUtil.*;
import static org.hameed.hameedmoneycli.constants.CommandConstants.*;

@Configuration
@RequiredArgsConstructor
public class ReportCommands {

    private final ReportService reportService;
    private final AccountService accountService;
    private final HmcConfig hmcConfig;

    @Bean
    public Command reportNetworth() {
        return Command.builder()
                .name("report nw")
                .description(REPORT_NW_COMMAND_DESCRIPTION)
                .help(REPORT_NW_COMMAND_HELP)
                .options(CommandOption.with()
                        .shortName('c')
                        .longName(CURRENCY_ARG)
                        .required(false)
                        .type(String.class)
                        .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String currency = argOrOption(ctx, 0, 'c', CURRENCY_ARG, hmcConfig.getDefaultCurrency());
                    reportService.generateNetworthReport(currency).terminalPrint(ctx.outputWriter());
                });
    }

    @Bean
    public Command reportDataIntegrity() {
        return Command.builder()
                .name("report data-integrity")
                .description(REPORT_DATA_INTEGRITY_COMMAND_DESCRIPTION)
                .help(REPORT_DATA_INTEGRITY_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    reportService.generateDataIntegrityReport().terminalPrint(ctx.outputWriter());
                });
    }

    @Bean
    public Command reportIncome() {
        return Command.builder()
                .name("report income")
                .description(REPORT_INCOME_COMMAND_DESCRIPTION)
                .help(REPORT_INCOME_COMMAND_HELP)
                .options(CommandOption.with()
                        .shortName('c')
                        .longName(CURRENCY_ARG)
                        .required(false)
                        .type(String.class)
                        .build(),
                        CommandOption.with()
                                .shortName('p')
                                .longName(PERIOD_ARG)
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
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    // Positional tokens: a period preset (e.g. last-month) or a currency (e.g. USD), in any order.
                    String positionalCurrency = null;
                    String positionalPeriod = null;
                    for (var arg : ctx.parsedInput().arguments()) {
                        String token = arg.value();
                        if (token == null || token.isBlank()) {
                            continue;
                        }
                        if (ReportPeriod.isPreset(token)) {
                            if (positionalPeriod != null) {
                                throw new IllegalArgumentException("Only one period can be given");
                            }
                            positionalPeriod = token;
                        } else {
                            if (positionalCurrency != null) {
                                throw new IllegalArgumentException("Unexpected argument '" + token
                                        + "'. Valid periods: " + ReportPeriod.presetTokens());
                            }
                            positionalCurrency = token;
                        }
                    }

                    String currency = positionalCurrency != null
                            ? positionalCurrency
                            : getOptionOrDefault(ctx, 'c', CURRENCY_ARG, hmcConfig.getDefaultCurrency());
                    String preset = positionalPeriod != null
                            ? positionalPeriod
                            : getOptionOrDefault(ctx, 'p', PERIOD_ARG, null);
                    String startDate = getOptionOrDefault(ctx, 's', START_DATE_ARG, null);
                    String endDate = getOptionOrDefault(ctx, 'e', END_DATE_ARG, null);

                    ReportPeriod period = ReportPeriod.resolve(preset, startDate, endDate, LocalDate.now(DateUtil.zone()));
                    reportService.generateIncomeReport(currency, period).terminalPrint(ctx.outputWriter());
                });
    }


}
