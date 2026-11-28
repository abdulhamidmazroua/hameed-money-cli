package org.hameed.hameedmoneycomputer.market;

import org.hameed.hameedmoneycomputer.shared.ValidationException;
import org.hameed.hameedmoneycomputer.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MarketQuoteServiceTest extends IntegrationTest {

    @Autowired MarketQuoteService quotes;

    @Test
    void manualQuoteKeepsItsDate() {
        // Used to be overwritten with "now" by JPA auditing (@CreatedDate on quote_date).
        LocalDate date = LocalDate.of(2025, 11, 3);

        MarketQuoteDto saved = quotes.setMarketQuote(new MarketQuoteDto("EUR", "USD", new BigDecimal("1.10"), date));

        assertEquals(date, saved.date());
        assertEquals(date, quotes.getMarketQuote("EUR", "USD").getFirst().date());
    }

    @Test
    void rejectsNonPositivePrices() {
        assertThrows(ValidationException.class,
                () -> quotes.setMarketQuote(new MarketQuoteDto("EUR", "USD", BigDecimal.ZERO, null)));
    }
}
