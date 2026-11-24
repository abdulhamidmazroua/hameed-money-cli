package org.hameed.hameedmoneycomputer.service;

import org.hameed.hameedmoneycomputer.enums.AccountType;
import org.hameed.hameedmoneycomputer.model.dto.CashflowReport;
import org.hameed.hameedmoneycomputer.model.dto.DataIntegrityReport;
import org.hameed.hameedmoneycomputer.model.dto.IncomeReport;
import org.hameed.hameedmoneycomputer.model.dto.NetworthReport;
import org.hameed.hameedmoneycomputer.model.dto.ReportPeriod;
import org.hameed.hameedmoneycomputer.model.entity.Account;
import org.hameed.hameedmoneycomputer.repository.SourceSystemRepository;
import org.hameed.hameedmoneycomputer.repository.TransactionRepository;
import org.hameed.hameedmoneycomputer.support.IntegrationTest;
import org.hameed.hameedmoneycomputer.support.LedgerFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests: pin down how balances and reports behave today, so the refactor can't change them silently.
 * Uses USD/EUR from the seed; dates are fixed in the past.
 */
@Import(LedgerFixture.class)
class LedgerAndReportsTest extends IntegrationTest {

    private static final ReportPeriod NOV_2025 = new ReportPeriod(LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 30), "Custom");

    @Autowired LedgerFixture ledger;
    @Autowired ReportService reports;
    @Autowired TransactionRepository transactions;
    @Autowired SourceSystemRepository sources;

    Account bank;
    Account card;
    Account salary;
    Account food;

    @Autowired org.hameed.hameedmoneycomputer.repository.AccountRepository accounts;

    @Test
    void networthFailsWhenAnyAccountLacksAConversionPath() {
        // Known gap: even zero-balance accounts (e.g. seeded ones) need a quote path, or the whole report fails.
        assertThrows(IllegalArgumentException.class, () -> reports.generateNetworthReport("USD"));
    }

    /** Give every currency used by a seeded internal account a dummy 1:1 path to USD. */
    private void connectSeededCurrencies() {
        accounts.getLeafAccounts().stream()
                .filter(a -> a.getMasterType() == AccountType.ASSET || a.getMasterType() == AccountType.LIABILITY)
                .map(a -> a.getAsset().getSymbol())
                .distinct()
                .filter(s -> !s.equals("USD") && !s.equals("EUR"))
                .forEach(s -> ledger.quote(s, "USD", "1"));
    }

    @BeforeEach
    void ledger() {
        bank = ledger.assetWithOpening("Test Bank", "USD", "1000");
        card = ledger.account("Test Card", AccountType.LIABILITY, "USD");
        salary = ledger.account("Test Salary", AccountType.INCOME, "USD");
        food = ledger.account("Test Food", AccountType.EXPENSE, "USD");

        ledger.move(salary, bank, "3000", "01-11-2025");   // income
        ledger.move(bank, food, "200", "05-11-2025");      // spending from bank
        ledger.move(card, food, "100", "10-11-2025");      // spending on card (debt grows)
        ledger.move(bank, card, "50", "20-11-2025");       // partial card payment (transfer)
        ledger.move(food, bank, "20", "21-11-2025");       // refund
        ledger.move(salary, bank, "999", "01-12-2025");    // outside the November period
    }

    private BigDecimal balance(Account a) {
        return transactions.getAccountBalance(a.getId());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }

    @Test
    void manualEntrySourceIsNotSeeded() {
        // Known gap (fixed in a later phase): a fresh database lacks MANUAL_ENTRY, so `account init` fails
        // until it is created. The fixture creates it; this documents the dependency.
        assertTrue(sources.existsByCode(LedgerFixture.MANUAL));
    }

    @Test
    void balancesFollowTheTypeRules() {
        assertAmount("4769", balance(bank));   // 1000 + 3000 - 200 - 50 + 20 + 999
        assertAmount("50", balance(card));     // debt: 100 borrowed - 50 repaid
        assertAmount("3999", balance(salary)); // earned
        assertAmount("280", balance(food));    // 200 + 100 - 20 refund
    }

    @Test
    void openingBalanceIsASystemAdjustment() {
        DataIntegrityReport integrity = reports.generateDataIntegrityReport();
        assertTrue(integrity.openingBalances().count() >= 1);
        assertTrue(integrity.openingBalances().breakdown().stream()
                .anyMatch(l -> l.symbol().equals("USD") && l.amount().compareTo(new BigDecimal("1000")) >= 0));
    }

    @Test
    void networthIsAssetsMinusLiabilities() {
        connectSeededCurrencies();
        NetworthReport nw = reports.generateNetworthReport("USD");

        assertTrue(nw.assetLines().stream().anyMatch(l -> l.accountName().equals("USD:Test Bank")));
        assertTrue(nw.liabilityLines().stream().anyMatch(l -> l.accountName().equals("USD:Test Card")));
        assertAmount(nw.totalAssets().subtract(nw.totalLiabilities()).toPlainString(), nw.netWorth());
    }

    @Test
    void networthConvertsThroughQuotes() {
        connectSeededCurrencies();
        ledger.quote("EUR", "USD", "1.10");
        Account euro = ledger.assetWithOpening("Test Euro", "EUR", "100");

        NetworthReport inEur = reports.generateNetworthReport("EUR");
        NetworthReport.NetworthLine bankLine = inEur.assetLines().stream()
                .filter(l -> l.accountName().equals("USD:Test Bank")).findFirst().orElseThrow();
        NetworthReport.NetworthLine euroLine = inEur.assetLines().stream()
                .filter(l -> l.accountName().equals(euro.getName())).findFirst().orElseThrow();

        assertAmount("4769", bankLine.nativeBalance());
        assertEquals(0, new BigDecimal("4335.45").compareTo(bankLine.convertedBalance().setScale(2, java.math.RoundingMode.HALF_UP)));
        assertAmount("100", euroLine.convertedBalance());
    }

    @Test
    void incomeReportSumsIncomeInPeriod() {
        IncomeReport income = reports.generateIncomeReport("USD", NOV_2025);

        IncomeReport.IncomeLine line = income.lines().stream()
                .filter(l -> l.accountName().equals("USD:Test Salary")).findFirst().orElseThrow();
        assertAmount("3000", line.nativeAmount());
    }

    @Test
    void cashflowExcludesTransfersAndOpeningBalances() {
        CashflowReport cf = reports.generateCashflowReport("USD", NOV_2025);

        CashflowReport.FlowLine foodLine = cf.expenseLines().stream()
                .filter(l -> l.accountName().equals("USD:Test Food")).findFirst().orElseThrow();
        assertAmount("280", foodLine.nativeAmount());   // card spend counts; card payment and opening don't

        // Only this test's data has transactions (the seed has none), so totals are exact.
        assertAmount("3000", cf.totalIncome());
        assertAmount("280", cf.totalExpenses());
        assertAmount("2720", cf.netCashFlow());
        assertAmount("90.6667", cf.savingsRate().setScale(4, java.math.RoundingMode.HALF_UP));
        assertAmount("0.9856", cf.elapsedMonths());   // 30 days / 30.436875
        assertAmount("284.09", cf.avgMonthlyBurn().setScale(2, java.math.RoundingMode.HALF_UP));
    }
}
