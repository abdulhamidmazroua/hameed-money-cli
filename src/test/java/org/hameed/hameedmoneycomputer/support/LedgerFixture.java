package org.hameed.hameedmoneycomputer.support;

import org.hameed.hameedmoneycomputer.enums.AccountType;
import org.hameed.hameedmoneycomputer.enums.TransactionType;
import org.hameed.hameedmoneycomputer.model.dto.AccountCreateDto;
import org.hameed.hameedmoneycomputer.model.dto.MarketQuoteDto;
import org.hameed.hameedmoneycomputer.model.dto.TransactionCreateDto;
import org.hameed.hameedmoneycomputer.model.entity.Account;
import org.hameed.hameedmoneycomputer.repository.SourceSystemRepository;
import org.hameed.hameedmoneycomputer.service.AccountService;
import org.hameed.hameedmoneycomputer.service.AssetService;
import org.hameed.hameedmoneycomputer.service.MarketQuoteService;
import org.hameed.hameedmoneycomputer.service.SourceService;
import org.hameed.hameedmoneycomputer.service.TransactionService;
import org.springframework.boot.test.context.TestComponent;

import java.math.BigDecimal;

/**
 * Builds ledger data for integration tests through the real services. Each test calls {@link #reset()} first.
 */
@TestComponent
public class LedgerFixture {

    public static final String MANUAL = "MANUAL_ENTRY";

    private final AccountService accountService;
    private final AssetService assetService;
    private final TransactionService transactionService;
    private final MarketQuoteService marketQuoteService;
    private final SourceService sourceService;
    private final SourceSystemRepository sourceSystemRepository;

    public LedgerFixture(AccountService accountService, AssetService assetService,
                         TransactionService transactionService, MarketQuoteService marketQuoteService,
                         SourceService sourceService, SourceSystemRepository sourceSystemRepository) {
        this.accountService = accountService;
        this.assetService = assetService;
        this.transactionService = transactionService;
        this.marketQuoteService = marketQuoteService;
        this.sourceService = sourceService;
        this.sourceSystemRepository = sourceSystemRepository;
    }

    /** The seed doesn't create MANUAL_ENTRY, but opening balances and manual transactions require it. */
    public void ensureManualSource() {
        if (!sourceSystemRepository.existsByCode(MANUAL)) {
            sourceService.addSource("Manual Entry", MANUAL, null, null);
        }
    }

    public Account account(String name, AccountType type, String assetSymbol) {
        Long assetId = assetService.getAssetBySymbol(assetSymbol).getId();
        boolean internal = type == AccountType.ASSET || type == AccountType.LIABILITY;
        return accountService.createAccount(new AccountCreateDto(name, type, null, assetId, internal));
    }

    public Account assetWithOpening(String name, String assetSymbol, String balance) {
        ensureManualSource();
        return accountService.createAccountWithOpeningBalance(name, null, assetSymbol, balance);
    }

    /** Same-currency movement of {@code amount} on {@code date} (dd-MM-yyyy). */
    public void move(Account from, Account to, String amount, String date) {
        move(from, to, amount, amount, date);
    }

    public void move(Account from, Account to, String fromAmount, String toAmount, String date) {
        ensureManualSource();
        transactionService.createTransaction(new TransactionCreateDto(
                "test " + from.getName() + " -> " + to.getName(),
                TransactionType.BANK_TRANSFER,
                from.getId(), to.getId(),
                new BigDecimal(fromAmount), new BigDecimal(toAmount),
                date, MANUAL, BigDecimal.ZERO));
    }

    /** One {@code base} is worth {@code price} {@code quote}. */
    public void quote(String base, String quote, String price) {
        marketQuoteService.setMarketQuote(new MarketQuoteDto(base, quote, new BigDecimal(price), null));
    }
}
