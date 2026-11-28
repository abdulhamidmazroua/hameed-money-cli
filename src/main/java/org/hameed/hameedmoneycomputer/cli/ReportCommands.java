package org.hameed.hameedmoneycomputer.cli;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.reporting.ReportPeriod;
import org.hameed.hameedmoneycomputer.reporting.ReportService;
import org.hameed.hameedmoneycomputer.shared.DateUtil;
import org.hameed.hameedmoneycomputer.shared.ValidationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;

import static org.hameed.hameedmoneycomputer.cli.CommandsUtil.*;
import static org.hameed.hameedmoneycomputer.cli.CommandConstants.*;

@Configuration
@RequiredArgsConstructor
public class ReportCommands {

    private final ReportService reportService;

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
                    String currency = argOrOption(ctx, 0, 'c', CURRENCY_ARG);
                    ReportPrinter.print(ctx.outputWriter(), reportService.generateNetworthReport(currency));
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
                    ReportPrinter.print(ctx.outputWriter(), reportService.generateDataIntegrityReport());
                });
    }

    @Bean
    public Command reportIncome() {
        return Command.builder()
                .name("report income")
                .description(REPORT_INCOME_COMMAND_DESCRIPTION)
                .help(REPORT_INCOME_COMMAND_HELP)
                .options(periodReportOptions())
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    CurrencyAndPeriod args = resolveCurrencyAndPeriod(ctx);
                    ReportPrinter.print(ctx.outputWriter(), reportService.generateIncomeReport(args.currency(), args.period()));
                });
    }

    @Bean
    public Command reportCashflow() {
        return Command.builder()
                .name("report cashflow")
                .description(REPORT_CASHFLOW_COMMAND_DESCRIPTION)
                .help(REPORT_CASHFLOW_COMMAND_HELP)
                .options(periodReportOptions())
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    CurrencyAndPeriod args = resolveCurrencyAndPeriod(ctx);
                    ReportPrinter.print(ctx.outputWriter(), reportService.generateCashflowReport(args.currency(), args.period()));
                });
    }

    /** {@code currency} is null when not given; the report then uses the base currency. */
    private record CurrencyAndPeriod(String currency, ReportPeriod period) {}

    private static CommandOption[] periodReportOptions() {
        return new CommandOption[]{
                CommandOption.with().shortName('c').longName(CURRENCY_ARG).required(false).type(String.class).build(),
                CommandOption.with().shortName('p').longName(PERIOD_ARG).required(false).type(String.class).build(),
                CommandOption.with().shortName('s').longName(START_DATE_ARG).required(false).type(String.class).build(),
                CommandOption.with().shortName('e').longName(END_DATE_ARG).required(false).type(String.class).build()
        };
    }

    /** Positional tokens may be a period preset (e.g. last-month) or a currency (e.g. USD), in any order. */
    private CurrencyAndPeriod resolveCurrencyAndPeriod(CommandContext ctx) {
        String positionalCurrency = null;
        String positionalPeriod = null;
        for (var arg : ctx.parsedInput().arguments()) {
            String token = arg.value();
            if (token == null || token.isBlank()) {
                continue;
            }
            if (ReportPeriod.isPreset(token)) {
                if (positionalPeriod != null) {
                    throw new ValidationException("Only one period can be given");
                }
                positionalPeriod = token;
            } else {
                if (positionalCurrency != null) {
                    throw new ValidationException("Unexpected argument '" + token
                            + "'. Valid periods: " + ReportPeriod.presetTokens());
                }
                positionalCurrency = token;
            }
        }

        String currency = positionalCurrency != null
                ? positionalCurrency
                : getOptionOrDefault(ctx, 'c', CURRENCY_ARG, null);
        String preset = positionalPeriod != null
                ? positionalPeriod
                : getOptionOrDefault(ctx, 'p', PERIOD_ARG, null);
        String startDate = getOptionOrDefault(ctx, 's', START_DATE_ARG, null);
        String endDate = getOptionOrDefault(ctx, 'e', END_DATE_ARG, null);

        ReportPeriod period = ReportPeriod.resolve(preset, startDate, endDate, DateUtil.today());
        return new CurrencyAndPeriod(currency, period);
    }
}
