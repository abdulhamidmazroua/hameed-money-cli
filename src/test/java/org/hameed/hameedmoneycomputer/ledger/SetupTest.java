package org.hameed.hameedmoneycomputer.ledger;

import org.hameed.hameedmoneycomputer.market.AssetCategory;
import org.hameed.hameedmoneycomputer.market.AssetFilter;
import org.hameed.hameedmoneycomputer.market.AssetService;
import org.hameed.hameedmoneycomputer.market.AssetView;
import org.hameed.hameedmoneycomputer.reporting.ReportService;
import org.hameed.hameedmoneycomputer.shared.NotFoundException;
import org.hameed.hameedmoneycomputer.shared.ValidationException;
import org.hameed.hameedmoneycomputer.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * First run on a fresh database: the seed holds only currencies, and {@code setup} creates the starter tree.
 * Note: setup writes the base currency to the test config.json, which is not rolled back with the transaction.
 */
class SetupTest extends IntegrationTest {

    @Autowired SetupService setup;
    @Autowired AccountService accounts;
    @Autowired AssetService assets;
    @Autowired ReportService reports;

    @Test
    void seedHasIsoCurrenciesButNoAccounts() {
        Set<String> currencies = assets.findAssets(new AssetFilter(null, AssetCategory.CASH, null)).stream()
                .map(AssetView::symbol)
                .collect(Collectors.toSet());

        assertTrue(currencies.size() > 150, "expected the active ISO 4217 list, got " + currencies.size());
        assertTrue(currencies.containsAll(List.of("USD", "EUR", "JPY", "XOF", "ZWG")));
        assertFalse(currencies.contains("XAU"), "metals are not currencies");
        assertFalse(currencies.contains("HRK"), "retired currencies are excluded");
        assertTrue(accounts.getAllAccounts().isEmpty());
    }

    @Test
    void setupCreatesStarterAccountsInTheBaseCurrency() {
        SetupService.SetupResult result = setup.setup(" eur ");

        assertEquals("EUR", result.baseCurrency());
        assertTrue(setup.isSetUp());
        assertTrue(result.createdAccounts().containsAll(List.of("Cash", "Debt", "EUR:Salary", "EUR:Groceries")));

        AccountView groceries = accounts.getAccountByName("EUR:Groceries");
        assertEquals(AccountType.EXPENSE, groceries.type());
        assertEquals("EUR", groceries.assetSymbol());
        assertTrue(accounts.getAccountByName("Cash").folder());

        // Running it again only changes the base currency.
        assertTrue(setup.setup("USD").createdAccounts().isEmpty());
    }

    @Test
    void reportsDefaultToTheBaseCurrency() {
        setup.setup("JPY");

        assertEquals("JPY", reports.generateNetworthReport(null).currency());
    }

    @Test
    void setupRejectsUnknownOrMissingCurrencies() {
        assertThrows(NotFoundException.class, () -> setup.setup("ZZZ"));
        assertThrows(NotFoundException.class, () -> setup.setup("XAU"));
        assertThrows(ValidationException.class, () -> setup.setup(" "));
    }
}
