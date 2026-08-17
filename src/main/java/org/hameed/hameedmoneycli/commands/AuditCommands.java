package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.service.AccountService;
import org.hameed.hameedmoneycli.service.AuditService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;
import org.springframework.shell.jline.tui.component.flow.SelectItem;

import java.util.List;

import static org.hameed.hameedmoneycli.constants.CommandConstants.*;
import static org.hameed.hameedmoneycli.constants.CommandConstants.AUDIT_TRAIL_COMMAND_DESCRIPTION;
import static org.hameed.hameedmoneycli.constants.CommandConstants.AUDIT_TRAIL_COMMAND_HELP;
import static org.hameed.hameedmoneycli.constants.CommandConstants.ID_ARG;
import static org.hameed.hameedmoneycli.constants.CommandConstants.NAME_ARG;
import static org.hameed.hameedmoneycli.util.CommandsUtil.*;
import static org.hameed.hameedmoneycli.util.CommandsUtil.exceptionMapper;

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
                            accountId = Long.valueOf(raw);
                        } else {
                            accountId = accountService.getAccountByName(raw).getId();
                        }
                    } else {
                        String idStr = getOptionOrDefault(ctx, 'i', ID_ARG, null);
                        String name = getOptionOrDefault(ctx, 'n', NAME_ARG, null);
                        if (idStr != null) {
                            accountId = Long.valueOf(idStr);
                        } else if (name != null) {
                            accountId = accountService.getAccountByName(name).getId();
                        }
                    }

                    if (accountId == null) {
                        List<SelectItem> choices = accountService.getAllAccounts().stream()
                                .map(a -> SelectItem.of(
                                        a.getName() + " (ID: " + a.getId() + ")",
                                        a.getId().toString()))
                                .toList();
                        ComponentFlow.ComponentFlowResult result = componentFlowBuilder.clone().reset()
                                .withSingleItemSelector("accountId")
                                .name("Select account to audit:")
                                .selectItems(choices)
                                .and().build().run();
                        accountId = Long.valueOf(result.getContext().get("accountId", String.class));
                    }

                    ctx.outputWriter().println(auditService.auditAccount(accountId));
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
                    ctx.outputWriter().println(auditService.auditTrail());
                });
    }
}
