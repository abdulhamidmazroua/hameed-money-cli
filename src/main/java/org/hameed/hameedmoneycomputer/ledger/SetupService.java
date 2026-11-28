package org.hameed.hameedmoneycomputer.ledger;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.market.Asset;
import org.hameed.hameedmoneycomputer.market.AssetCategory;
import org.hameed.hameedmoneycomputer.market.AssetStore;
import org.hameed.hameedmoneycomputer.shared.HmcConfig;
import org.hameed.hameedmoneycomputer.shared.NotFoundException;
import org.hameed.hameedmoneycomputer.shared.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * First-run setup: chooses the base currency (the default for reports) and, when the ledger is still empty,
 * creates a small, neutral set of starter accounts in it. Running it again only changes the base currency.
 */
@Service
@RequiredArgsConstructor
public class SetupService {

    private static final List<String> ASSET_FOLDERS = List.of("Cash", "Securities", "Property");
    private static final List<String> LIABILITY_FOLDERS = List.of("Debt");
    private static final List<String> INCOME = List.of("Salary", "Bonus", "Other Income");
    private static final List<String> EXPENSES = List.of(
            "Housing", "Utilities", "Groceries", "Dining", "Transport", "Health",
            "Subscriptions", "Family", "Charity", "Other");

    private final HmcConfig config;
    private final AssetStore assets;
    private final AccountStore accounts;
    private final AccountService accountService;

    public boolean isSetUp() {
        return config.baseCurrency().isPresent();
    }

    @Transactional
    public SetupResult setup(String baseCurrency) {
        String symbol = baseCurrency == null ? "" : baseCurrency.trim().toUpperCase(Locale.ROOT);
        if (symbol.isEmpty()) {
            throw new ValidationException("Base currency is required: an ISO 4217 code such as USD");
        }
        Asset currency = assets.findBySymbolAndCategory(symbol, AssetCategory.CASH)
                .orElseThrow(() -> new NotFoundException("Unknown currency '" + symbol
                        + "'. Use an ISO 4217 code such as USD (list them with `asset find --category cash`)."));

        List<String> created = accounts.findAll().isEmpty() ? createStarterAccounts(currency) : List.of();
        config.setBaseCurrency(symbol);
        return new SetupResult(symbol, created);
    }

    private List<String> createStarterAccounts(Asset currency) {
        List<String> created = new ArrayList<>();
        ASSET_FOLDERS.forEach(name -> createIfMissing(name, AccountType.ASSET, null, created));
        LIABILITY_FOLDERS.forEach(name -> createIfMissing(name, AccountType.LIABILITY, null, created));
        INCOME.forEach(name -> createIfMissing(name, AccountType.INCOME, currency, created));
        EXPENSES.forEach(name -> createIfMissing(name, AccountType.EXPENSE, currency, created));
        return created;
    }

    private void createIfMissing(String name, AccountType type, Asset asset, List<String> created) {
        String fullName = asset == null ? name : asset.getSymbol() + ":" + name;
        if (accounts.findByName(fullName).isEmpty()) {
            Long assetId = asset == null ? null : asset.getId();
            created.add(accountService.createAccount(
                    new AccountCreateDto(name, type, null, assetId, type.isInternal())).name());
        }
    }

    /** @param createdAccounts names of the starter accounts created (empty when the ledger already had accounts) */
    public record SetupResult(String baseCurrency, List<String> createdAccounts) {}
}
