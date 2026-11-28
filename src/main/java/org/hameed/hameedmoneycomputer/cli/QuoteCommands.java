package org.hameed.hameedmoneycomputer.cli;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.market.MarketQuoteDto;
import org.hameed.hameedmoneycomputer.market.MarketQuoteService;
import org.hameed.hameedmoneycomputer.shared.DateUtil;
import org.hameed.hameedmoneycomputer.shared.ValidationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;

import java.util.List;

import static org.hameed.hameedmoneycomputer.cli.CommandsUtil.*;
import static org.hameed.hameedmoneycomputer.cli.CommandConstants.*;

@Configuration
@RequiredArgsConstructor
public class QuoteCommands {

    private final MarketQuoteService marketQuoteService;

    @Bean
    public Command setQuote() {
        return Command.builder()
                .name("quote set")
                .description(QUOTE_SET_COMMAND_DESCRIPTION)
                .help(QUOTE_SET_COMMAND_HELP)
                .options(
                        CommandOption.with()
                                .shortName('b')
                                .longName(BASE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('q')
                                .longName(QUOTE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('p')
                                .longName(PRICE_ARG)
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('d')
                                .longName(DATE_ARG)
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String baseSymbol = argOrOption(ctx, 0, 'b', BASE_ARG);
                    if (baseSymbol == null) throw new ValidationException(QUOTE_SET_BASE_ARG_ERROR);
                    String quoteSymbol = argOrOption(ctx, 1, 'q', QUOTE_ARG);
                    if (quoteSymbol == null) throw new ValidationException(QUOTE_SET_QUOTE_ARG_ERROR);
                    String price = argOrOption(ctx, 2, 'p', PRICE_ARG);
                    if (price == null) throw new ValidationException(QUOTE_SET_PRICE_ARG_ERROR);
                    String date = getOptionOrDefault(ctx, 'd', DATE_ARG, null);
                    marketQuoteService.setMarketQuote(new MarketQuoteDto(
                            baseSymbol,
                            quoteSymbol,
                            parseAmount(price, PRICE_ARG),
                            date == null ? null : DateUtil.parseUserDate(date)
                    ));
                });
    }

    @Bean
    public Command getQuote() {
        return Command.builder()
                .name("quote get")
                .description(QUOTE_GET_COMMAND_DESCRIPTION)
                .help(QUOTE_GET_COMMAND_HELP)
                .options(
                        CommandOption.with()
                                .shortName('b')
                                .longName(BASE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('q')
                                .longName(QUOTE_ARG)
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String baseSymbol = argOrOption(ctx, 0, 'b', BASE_ARG);
                    if (baseSymbol == null) throw new ValidationException(QUOTE_GET_BASE_ARG_ERROR);
                    String quoteSymbol = argOrOption(ctx, 1, 'q', QUOTE_ARG);
                    if (quoteSymbol == null) throw new ValidationException(QUOTE_GET_QUOTE_ARG_ERROR);

                    List<MarketQuoteDto> marketQuotes = marketQuoteService.getMarketQuote(baseSymbol, quoteSymbol);
                    marketQuotes.forEach(quote -> ctx.outputWriter().println("Price of " + quote.baseSymbol() + " in " + quote.quoteSymbol() + " is " + quote.price() + " (as of " + DateUtil.formatDate(quote.date()) + ")"));
                });
    }

    @Bean
    public Command fetchQuote() {
        return Command.builder()
                .name("quote fetch")
                .description(QUOTE_FETCH_COMMAND_DESCRIPTION)
                .help(QUOTE_FETCH_COMMAND_HELP)
                .options(
                        CommandOption.with()
                                .shortName('b')
                                .longName(BASE_ARG)
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('q')
                                .longName(QUOTE_ARG)
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String baseSymbol = argOrOption(ctx, 0, 'b', BASE_ARG);
                    if (baseSymbol == null) throw new ValidationException(QUOTE_FETCH_BASE_ARG_ERROR);
                    String quoteSymbol = argOrOption(ctx, 1, 'q', QUOTE_ARG);
                    if (quoteSymbol == null) throw new ValidationException(QUOTE_FETCH_QUOTE_ARG_ERROR);

                    marketQuoteService.fetchAndSaveQuote(baseSymbol, quoteSymbol);
                    ctx.outputWriter().println("Saved quote: " + baseSymbol + " -> " + quoteSymbol);
                });
    }

    @Bean
    public Command refreshQuotes() {
        return Command.builder()
                .name("quote refresh")
                .description(QUOTE_REFRESH_COMMAND_DESCRIPTION)
                .help(QUOTE_REFRESH_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    var result = marketQuoteService.refreshAllQuotes();

                    if (result.updated().isEmpty() && result.failed().isEmpty()) {
                        ctx.outputWriter().println("No stored quotes to refresh.");
                        return;
                    }

                    for (String pair : result.updated()) {
                        ctx.outputWriter().println("Updated: " + pair);
                    }

                    for (var failure : result.failed()) {
                        ctx.outputWriter().println("FAILED: " + failure.baseSymbol() + " -> " + failure.quoteSymbol()
                                + " (" + failure.reason() + ")");
                    }

                    ctx.outputWriter().println("Done. " + result.updated().size() + " updated, "
                            + result.failed().size() + " failed.");
                });
    }

    @Bean
    public Command listQuotes() {
        return Command.builder()
                .name("quote list")
                .description(QUOTE_LIST_COMMAND_DESCRIPTION)
                .help(QUOTE_LIST_COMMAND_HELP)
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    List<MarketQuoteDto> quotes = marketQuoteService.listMarketQuotes();
                    if (quotes.isEmpty()) {
                        ctx.outputWriter().println("No market quotes stored.");
                        return;
                    }
                    String format = "%-12s %-12s %-12s %s";
                    ctx.outputWriter().println(String.format(format, "Base", "Quote", "Price", "Date"));
                    ctx.outputWriter().println(String.format(format, "----", "-----", "-----", "----"));
                    for (MarketQuoteDto q : quotes) {
                        ctx.outputWriter().println(String.format(format, q.baseSymbol(), q.quoteSymbol(), q.price(), DateUtil.formatDate(q.date())));
                    }
                });
    }
}
