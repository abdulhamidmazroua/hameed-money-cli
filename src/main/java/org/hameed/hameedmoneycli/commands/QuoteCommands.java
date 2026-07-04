package org.hameed.hameedmoneycli.commands;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.model.dto.MarketQuoteDto;
import org.hameed.hameedmoneycli.service.MarketQuoteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;

import java.math.BigDecimal;
import java.util.List;

import static org.hameed.hameedmoneycli.util.CommandsUtil.*;

@Configuration
@RequiredArgsConstructor
public class QuoteCommands {

    private final MarketQuoteService marketQuoteService;

    @Bean
    public Command setQuote() {
        return Command.builder()
                .name("quote set")
                .description("Set a market quote manually")
                .help("Set a market quote manually. Usage: `quote set USD EGP --price 48.5` or `quote set --base USD --quote EGP --price 48.5`")
                .options(
                        CommandOption.with()
                                .shortName('b')
                                .longName("base")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('q')
                                .longName("quote")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('p')
                                .longName("price")
                                .required(true)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('d')
                                .longName("date")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String baseSymbol = argOrOption(ctx, 0, 'b', "base");
                    if (baseSymbol == null) throw new IllegalArgumentException(required("base"));
                    String quoteSymbol = argOrOption(ctx, 1, 'q', "quote");
                    if (quoteSymbol == null) throw new IllegalArgumentException(required("quote"));
                    String price = getOptionOrError(ctx, 'p', "price", required("price"));
                    String date = getOptionOrDefault(ctx, 'd', "date", null);
                    marketQuoteService.setMarketQuote(new MarketQuoteDto(
                            baseSymbol,
                            quoteSymbol,
                            new BigDecimal(price),
                            date
                    ));
                });
    }

    @Bean
    public Command getQuote() {
        return Command.builder()
                .name("quote get")
                .description("Look up a stored market quote")
                .help("Look up a stored market quote. Usage: `quote get AAPL USD` or `quote get --base AAPL --quote USD`")
                .options(
                        CommandOption.with()
                                .shortName('b')
                                .longName("base")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('q')
                                .longName("quote")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String baseSymbol = argOrOption(ctx, 0, 'b', "base");
                    if (baseSymbol == null) throw new IllegalArgumentException(required("base"));
                    String quoteSymbol = argOrOption(ctx, 1, 'q', "quote");
                    if (quoteSymbol == null) throw new IllegalArgumentException(required("quote"));

                    List<MarketQuoteDto> marketQuotes = marketQuoteService.getMarketQuote(baseSymbol, quoteSymbol);
                    marketQuotes.forEach(quote -> ctx.outputWriter().println("Price of " + quote.baseSymbol() + " in " + quote.quoteSymbol() + " is " + quote.price() + " (as of " + quote.marketQuoteDate() + ")"));
                });
    }

    @Bean
    public Command fetchQuote() {
        return Command.builder()
                .name("quote fetch")
                .description("Fetch the latest quote from Yahoo Finance and save it")
                .help("Fetch the latest quote from Yahoo Finance and save it. Usage: `quote fetch AAPL USD` or `quote fetch --base AAPL --quote USD`")
                .options(
                        CommandOption.with()
                                .shortName('b')
                                .longName("base")
                                .required(false)
                                .type(String.class)
                                .build(),
                        CommandOption.with()
                                .shortName('q')
                                .longName("quote")
                                .required(false)
                                .type(String.class)
                                .build()
                )
                .availabilityProvider(availabilityProvider())
                .exitStatusExceptionMapper(exceptionMapper())
                .execute(ctx -> {
                    String baseSymbol = argOrOption(ctx, 0, 'b', "base");
                    if (baseSymbol == null) throw new IllegalArgumentException(required("base"));
                    String quoteSymbol = argOrOption(ctx, 1, 'q', "quote");
                    if (quoteSymbol == null) throw new IllegalArgumentException(required("quote"));

                    marketQuoteService.fetchAndSaveQuote(baseSymbol, quoteSymbol);
                    ctx.outputWriter().println("Saved quote: " + baseSymbol + " -> " + quoteSymbol);
                });
    }

    @Bean
    public Command listQuotes() {
        return Command.builder()
                .name("quote list")
                .description("List all stored market quotes")
                .help("List the latest quote for each asset pair. Usage: `quote list`")
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
                        ctx.outputWriter().println(String.format(format, q.baseSymbol(), q.quoteSymbol(), q.price(), q.marketQuoteDate()));
                    }
                });
    }
}
