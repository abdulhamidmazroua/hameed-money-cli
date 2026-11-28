package org.hameed.hameedmoneycomputer.reporting;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.ledger.Account;
import org.hameed.hameedmoneycomputer.ledger.AccountStore;
import org.hameed.hameedmoneycomputer.ledger.AccountType;
import org.hameed.hameedmoneycomputer.ledger.Transaction;
import org.hameed.hameedmoneycomputer.ledger.TransactionStore;
import org.hameed.hameedmoneycomputer.market.Asset;
import org.hameed.hameedmoneycomputer.market.AssetCategory;
import org.hameed.hameedmoneycomputer.market.AssetStore;
import org.hameed.hameedmoneycomputer.market.FinancialOracle;
import org.hameed.hameedmoneycomputer.market.MarketQuoteService;
import org.hameed.hameedmoneycomputer.shared.DateUtil;
import org.hameed.hameedmoneycomputer.shared.HmcConfig;
import org.hameed.hameedmoneycomputer.shared.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Financial reports. Each takes a report currency (an ISO code); null or blank means the base currency.
 * Amounts in other assets are converted with the latest market quotes.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final AccountStore accounts;
    private final TransactionStore transactions;
    private final AssetStore assets;
    private final MarketQuoteService marketQuoteService;
    private final HmcConfig config;

    public NetworthReport generateNetworthReport(String currency) {
        Asset targetAsset = reportCurrency(currency);
        FinancialOracle financialOracle = marketQuoteService.getFinancialOracle();

        Map<Long, BigDecimal> rateCache = new HashMap<>();
        List<NetworthReport.NetworthLine> assetLines = new ArrayList<>();
        List<NetworthReport.NetworthLine> liabilityLines = new ArrayList<>();

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (Account account : accounts.findLeaves()) {
            if (account.getMasterType() != AccountType.ASSET && account.getMasterType() != AccountType.LIABILITY) {
                continue;
            }

            BigDecimal balance = transactions.balanceOf(account.getId());
            BigDecimal converted = balance.multiply(rate(account, targetAsset, financialOracle, rateCache));

            NetworthReport.NetworthLine line = new NetworthReport.NetworthLine(
                    account.getName(), balance, converted, account.getAsset().getSymbol()
            );

            if (account.getMasterType() == AccountType.ASSET) {
                assetLines.add(line);
                totalAssets = totalAssets.add(converted);
            } else {
                liabilityLines.add(line);
                totalLiabilities = totalLiabilities.add(converted);
            }
        }

        return new NetworthReport(
                totalAssets,
                totalLiabilities,
                totalAssets.subtract(totalLiabilities),
                targetAsset.getSymbol(),
                assetLines,
                liabilityLines
        );
    }

    public DataIntegrityReport generateDataIntegrityReport() {
        List<Transaction> systemAdjustments = transactions.findSystemAdjustments();
        BigDecimal totalVolume = transactions.totalVolume();

        Map<String, BigDecimal> openingBreakdown = new HashMap<>();
        Map<String, BigDecimal> increaseBreakdown = new HashMap<>();
        Map<String, BigDecimal> decreaseBreakdown = new HashMap<>();

        for (Transaction tx : systemAdjustments) {
            String fromName = tx.getFromAccount().getName();
            String toName = tx.getToAccount().getName();

            if (fromName.contains("Opening ")) {
                String symbol = tx.getToAccount().getAsset().getSymbol();
                openingBreakdown.merge(symbol, tx.getToAmount(), BigDecimal::add);
            } else if (fromName.contains("Increase Adjustment")) {
                String symbol = tx.getToAccount().getAsset().getSymbol();
                increaseBreakdown.merge(symbol, tx.getToAmount(), BigDecimal::add);
            }

            if (toName.contains("Decrease Adjustment")) {
                String symbol = tx.getFromAccount().getAsset().getSymbol();
                decreaseBreakdown.merge(symbol, tx.getFromAmount(), BigDecimal::add);
            }
        }

        DataIntegrityReport.Section openingSection = buildSection(openingBreakdown);
        DataIntegrityReport.Section increaseSection = buildSection(increaseBreakdown);
        DataIntegrityReport.Section decreaseSection = buildSection(decreaseBreakdown);

        BigDecimal totalAdjustments = openingSection.total().add(increaseSection.total()).add(decreaseSection.total());
        BigDecimal health;
        if (totalVolume.compareTo(BigDecimal.ZERO) == 0) {
            health = BigDecimal.ONE;
        } else {
            health = BigDecimal.ONE.subtract(
                    totalAdjustments.divide(totalVolume, 4, RoundingMode.HALF_UP)
            );
        }

        return new DataIntegrityReport(openingSection, increaseSection, decreaseSection, health);
    }

    public IncomeReport generateIncomeReport(String currency, ReportPeriod period) {
        Asset targetAsset = reportCurrency(currency);
        FinancialOracle financialOracle = marketQuoteService.getFinancialOracle();

        Map<Long, BigDecimal> rateCache = new HashMap<>();
        List<CashflowReport.FlowLine> flows = flowLines(AccountType.INCOME, false, period, targetAsset, financialOracle, rateCache);

        List<IncomeReport.IncomeLine> lines = flows.stream()
                .map(f -> new IncomeReport.IncomeLine(f.accountName(), f.nativeAmount(), f.convertedAmount(), f.assetSymbol()))
                .toList();
        return new IncomeReport(sumConverted(flows), targetAsset.getSymbol(), period, lines);
    }

    public CashflowReport generateCashflowReport(String currency, ReportPeriod period) {
        Asset targetAsset = reportCurrency(currency);
        FinancialOracle financialOracle = marketQuoteService.getFinancialOracle();
        Map<Long, BigDecimal> rateCache = new HashMap<>();

        List<CashflowReport.FlowLine> incomeLines =
                flowLines(AccountType.INCOME, false, period, targetAsset, financialOracle, rateCache);
        List<CashflowReport.FlowLine> expenseLines =
                flowLines(AccountType.EXPENSE, true, period, targetAsset, financialOracle, rateCache);

        BigDecimal income = sumConverted(incomeLines);
        BigDecimal expenses = sumConverted(expenseLines);
        BigDecimal net = income.subtract(expenses);
        BigDecimal savingsRate = income.signum() > 0
                ? net.multiply(BigDecimal.valueOf(100)).divide(income, 4, RoundingMode.HALF_UP)
                : null;

        LocalDate today = DateUtil.today();
        LocalDate firstActivity = transactions.firstActivityDate().map(DateUtil::toLocalDate).orElse(null);
        BigDecimal months = period.elapsedMonths(firstActivity, today);
        BigDecimal burn = months.signum() > 0 ? expenses.divide(months, 4, RoundingMode.HALF_UP) : null;

        BigDecimal liquidCash = BigDecimal.ZERO;
        for (Account account : accounts.findLeaves()) {
            if (account.getMasterType() != AccountType.ASSET
                    || account.getAsset().getCategory() != AssetCategory.CASH) {
                continue;
            }
            BigDecimal balance = transactions.balanceOf(account.getId());
            liquidCash = liquidCash.add(balance.multiply(rate(account, targetAsset, financialOracle, rateCache)));
        }
        BigDecimal runway = burn != null && burn.signum() > 0
                ? liquidCash.max(BigDecimal.ZERO).divide(burn, 4, RoundingMode.HALF_UP)
                : null;

        return new CashflowReport(income, expenses, net, savingsRate, months, burn, liquidCash, runway,
                targetAsset.getSymbol(), period, incomeLines, expenseLines);
    }

    /** The CASH asset for {@code currency}, or for the base currency when none is given. */
    private Asset reportCurrency(String currency) {
        String symbol = (currency == null || currency.isBlank() ? config.requireBaseCurrency() : currency)
                .trim().toUpperCase(Locale.ROOT);
        return assets.findBySymbolAndCategory(symbol, AssetCategory.CASH)
                .orElseThrow(() -> new NotFoundException("Currency not found: " + symbol));
    }

    /**
     * Net flow per leaf account of {@code type} in the period, converted to the target asset and sorted largest first.
     * INCOME flows are Σfrom − Σto; with {@code negate} the sign flips to Σto − Σfrom, the EXPENSE balance rule.
     */
    private List<CashflowReport.FlowLine> flowLines(AccountType type, boolean negate, ReportPeriod period,
                                                    Asset targetAsset, FinancialOracle oracle,
                                                    Map<Long, BigDecimal> rateCache) {
        List<CashflowReport.FlowLine> lines = new ArrayList<>();
        for (TransactionStore.AccountFlow flow : transactions.netOutflowByAccountType(
                type, period.startMillis(), period.endExclusiveMillis())) {
            Account account = flow.account();
            BigDecimal net = negate ? flow.netOutflow().negate() : flow.netOutflow();
            if (net.signum() == 0) {
                continue;
            }
            BigDecimal converted = net.multiply(rate(account, targetAsset, oracle, rateCache));
            lines.add(new CashflowReport.FlowLine(account.getName(), net, converted, account.getAsset().getSymbol()));
        }
        lines.sort(Comparator.comparing(CashflowReport.FlowLine::convertedAmount).reversed());
        return lines;
    }

    private static BigDecimal rate(Account account, Asset targetAsset, FinancialOracle oracle,
                                   Map<Long, BigDecimal> rateCache) {
        return rateCache.computeIfAbsent(account.getAsset().getId(), key -> oracle.getRate(key, targetAsset.getId()));
    }

    private static BigDecimal sumConverted(List<CashflowReport.FlowLine> lines) {
        return lines.stream().map(CashflowReport.FlowLine::convertedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DataIntegrityReport.Section buildSection(Map<String, BigDecimal> breakdown) {
        int count = breakdown.size();
        BigDecimal total = breakdown.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<DataIntegrityReport.AssetLine> lines = breakdown.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DataIntegrityReport.AssetLine(e.getKey(), e.getValue()))
                .toList();
        return new DataIntegrityReport.Section(count, total, lines);
    }
}
