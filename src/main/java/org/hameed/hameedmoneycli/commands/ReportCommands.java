package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.service.AccountService;
import org.hameed.hameedmoneycli.service.AuditService;
import org.hameed.hameedmoneycli.service.ReportService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.util.List;

import static org.hameed.hameedmoneycli.util.CommandsUtil.*;
import static org.hameed.hameedmoneycli.constants.CommandConstants.*;

@Configuration
@RequiredArgsConstructor
public class ReportCommands {

    private final ReportService reportService;
    private final AccountService accountService;

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
                        .defaultValue("EGP")
                        .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String currency = argOrOption(ctx, 0, 'c', CURRENCY_ARG, "EGP");
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


}
