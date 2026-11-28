package org.hameed.hameedmoneycomputer.support;

import org.hameed.hameedmoneycomputer.ledger.AccountCreateDto;
import org.hameed.hameedmoneycomputer.ledger.AccountService;
import org.hameed.hameedmoneycomputer.ledger.AccountType;
import org.hameed.hameedmoneycomputer.ledger.AccountView;
import org.hameed.hameedmoneycomputer.ledger.TransactionCreateDto;
import org.hameed.hameedmoneycomputer.ledger.TransactionService;
import org.hameed.hameedmoneycomputer.ledger.TransactionType;
import org.hameed.hameedmoneycomputer.market.AssetService;
import org.hameed.hameedmoneycomputer.market.MarketQuoteDto;
import org.hameed.hameedmoneycomputer.market.MarketQuoteService;
import org.hameed.hameedmoneycomputer.shared.DateUtil;
import org.springframework.boot.test.context.TestComponent;

import java.math.BigDecimal;

/** Builds ledger data for integration tests through the real services. */
@TestComponent
public class LedgerFixture {

    public static final String MANUAL = "MANUAL_ENTRY";

    private final AccountService accountService;
    private final AssetService assetService;
    private final TransactionService transactionService;
    private final MarketQuoteService marketQuoteService;

    public LedgerFixture(AccountService accountService, AssetService assetService,
                         TransactionService transactionService, MarketQuoteService marketQuoteService) {
        this.accountService = accountService;
        this.assetService = assetService;
        this.transactionService = transactionService;
        this.marketQuoteService = marketQuoteService;
    }

    public AccountView account(String name, AccountType type, String assetSymbol) {
        long assetId = assetService.getAssetBySymbol(assetSymbol).id();
        boolean internal = type == AccountType.ASSET || type == AccountType.LIABILITY;
        return accountService.createAccount(new AccountCreateDto(name, type, null, assetId, internal));
    }

    public AccountView assetWithOpening(String name, String assetSymbol, String balance) {
        return accountService.createAccountWithOpeningBalance(name, null, assetSymbol, new BigDecimal(balance));
    }

    public BigDecimal balance(AccountView account) {
        return accountService.balance(account.id());
    }

    /** Same-currency movement of {@code amount} on {@code date} (dd-MM-yyyy). */
    public void move(AccountView from, AccountView to, String amount, String date) {
        move(from, to, amount, amount, date);
    }

    public void move(AccountView from, AccountView to, String fromAmount, String toAmount, String date) {
        transactionService.createTransaction(new TransactionCreateDto(
                "test " + from.name() + " -> " + to.name(),
                TransactionType.BANK_TRANSFER,
                from.id(), to.id(),
                new BigDecimal(fromAmount), new BigDecimal(toAmount),
                DateUtil.parseUserDate(date), MANUAL, BigDecimal.ZERO));
    }

    /** One {@code base} is worth {@code price} {@code quote}. */
    public void quote(String base, String quote, String price) {
        marketQuoteService.setMarketQuote(new MarketQuoteDto(base, quote, new BigDecimal(price), null));
    }
}
