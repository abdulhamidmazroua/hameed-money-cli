package org.hameed.hameedmoneycomputer.cli;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.ledger.AccountService;
import org.hameed.hameedmoneycomputer.reporting.AuditService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.util.List;

import static org.hameed.hameedmoneycomputer.cli.CommandConstants.*;
import static org.hameed.hameedmoneycomputer.cli.CommandsUtil.*;

@Configuration
@RequiredArgsConstructor
public class AuditCommands {

    private final AuditService auditService;
    private final ComponentFlow.Builder componentFlowBuilder;
    private final AccountService accountService;

    @Bean
    public Command auditAccount() {
        return Command.builder()
                .name("audit account")
                .description(AUDIT_ACCOUNT_COMMAND_DESCRIPTION)
                .help(AUDIT_ACCOUNT_COMMAND_HELP)
                .options(
                        CommandOption.with()
                                .shortName('i')
                                .longName(ID_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('n')
                                .longName(NAME_ARG)
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    var args = ctx.parsedInput().arguments();
                    Long accountId = null;
                    if (!args.isEmpty()) {
                        String raw = args.getFirst().value();
                        if (raw.matches("\\d+")) {
                            accountId = parseId(raw, ID_ARG);
                        } else {
                            accountId = accountService.getAccountByName(raw).id();
                        }
                    } else {
                        String idStr = getOptionOrDefault(ctx, 'i', ID_ARG, null);
                        String name = getOptionOrDefault(ctx, 'n', NAME_ARG, null);
                        if (idStr != null) {
                            accountId = parseId(idStr, ID_ARG);
                        } else if (name != null) {
                            accountId = accountService.getAccountByName(name).id();
                        }
                    }

                    if (accountId == null) {
                        List<SelectItem> choices = accountService.getAllAccounts().stream()
                                .map(a -> SelectItem.of(
                                        a.name() + " (ID: " + a.id() + ")",
                                        String.valueOf(a.id())))
                                .toList();
                        ComponentFlow.ComponentFlowResult result = componentFlowBuilder.clone().reset()
                                .withSingleItemSelector("accountId")
                                .name("Select account to audit:")
                                .selectItems(choices)
                                .and().build().run();
                        accountId = parseId(result.getContext().get("accountId", String.class), ID_ARG);
                    }

                    ReportPrinter.print(ctx.outputWriter(), auditService.auditAccount(accountId));
                });
    }

    @Bean
    public Command auditTrail() {
        return Command.builder()
                .name("audit trail")
                .description(AUDIT_TRAIL_COMMAND_DESCRIPTION)
                .help(AUDIT_TRAIL_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    ReportPrinter.print(ctx.outputWriter(), auditService.auditTrail());
                });
    }
}
