package org.hameed.hameedmoneycomputer.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.enums.AccountType;
import org.hameed.hameedmoneycomputer.enums.AssetCategory;
import org.hameed.hameedmoneycomputer.model.dto.CashflowReport;
import org.hameed.hameedmoneycomputer.model.dto.DataIntegrityReport;
import org.hameed.hameedmoneycomputer.model.dto.IncomeReport;
import org.hameed.hameedmoneycomputer.model.dto.NetworthReport;
import org.hameed.hameedmoneycomputer.model.dto.ReportPeriod;
import org.hameed.hameedmoneycomputer.model.entity.Account;
import org.hameed.hameedmoneycomputer.model.entity.Asset;
import org.hameed.hameedmoneycomputer.model.entity.FinancialOracle;
import org.hameed.hameedmoneycomputer.model.entity.Transaction;
import org.hameed.hameedmoneycomputer.repository.AccountRepository;
import org.hameed.hameedmoneycomputer.repository.AssetRepository;
import org.hameed.hameedmoneycomputer.repository.TransactionRepository;
import org.hameed.hameedmoneycomputer.util.DateUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {


    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AssetRepository assetRepository;
    private final MarketQuoteService marketQuoteService;

    public NetworthReport generateNetworthReport(String currency) {
        FinancialOracle financialOracle = marketQuoteService.getFinancialOracle();
        Asset targetAsset = assetRepository.findBySymbolAndCategory(currency, AssetCategory.CASH)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + currency));

        Map<Long, BigDecimal> rateCache = new HashMap<>();
        List<NetworthReport.NetworthLine> assetLines = new ArrayList<>();
        List<NetworthReport.NetworthLine> liabilityLines = new ArrayList<>();

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (Account account : accountRepository.getLeafAccounts()) {
            if (account.getMasterType() != AccountType.ASSET && account.getMasterType() != AccountType.LIABILITY) {
                continue;
            }

            BigDecimal balance = transactionRepository.getAccountBalance(account.getId());
            BigDecimal rate = rateCache.computeIfAbsent(
                    account.getAsset().getId(),
                    key -> financialOracle.getRate(key, targetAsset.getId())
            );
            BigDecimal converted = balance.multiply(rate);

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
                currency,
                assetLines,
                liabilityLines
        );
    }

    public DataIntegrityReport generateDataIntegrityReport() {
        List<Transaction> systemAdjustments = transactionRepository.findAllSystemAdjustments();
        BigDecimal totalVolume = transactionRepository.sumAllFromAmounts();

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
        String symbol = currency.trim().toUpperCase();
        FinancialOracle financialOracle = marketQuoteService.getFinancialOracle();
        Asset targetAsset = assetRepository.findBySymbolAndCategory(symbol, AssetCategory.CASH)
                .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + symbol));

        Map<Long, BigDecimal> rateCache = new HashMap<>();
        List<CashflowReport.FlowLine> flows = flowLines(AccountType.INCOME, false, period, targetAsset, financialOracle, rateCache);

        List<IncomeReport.IncomeLine> lines = flows.stream()
                .map(f -> new IncomeReport.IncomeLine(f.accountName(), f.nativeAmount(), f.convertedAmount(), f.assetSymbol()))
                .toList();
        return new IncomeReport(sumConverted(flows), symbol, period, lines);
    }

    public CashflowReport generateCashflowReport(String currency, ReportPeriod period) {
        String symbol = currency.trim().toUpperCase();
        FinancialOracle financialOracle = marketQuoteService.getFinancialOracle();
        Asset targetAsset = assetRepository.findBySymbolAndCategory(symbol, AssetCategory.CASH)
                .orElseThrow(() -> new IllegalArgumentException("Currency not found: " + symbol));
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

        LocalDate today = LocalDate.now(DateUtil.zone());
        Long firstMillis = transactionRepository.findFirstActivityDate();
        LocalDate firstActivity = firstMillis == null ? null
                : Instant.ofEpochMilli(firstMillis).atZone(DateUtil.zone()).toLocalDate();
        BigDecimal months = period.elapsedMonths(firstActivity, today);
        BigDecimal burn = months.signum() > 0 ? expenses.divide(months, 4, RoundingMode.HALF_UP) : null;

        BigDecimal liquidCash = BigDecimal.ZERO;
        for (Account account : accountRepository.getLeafAccounts()) {
            if (account.getMasterType() != AccountType.ASSET
                    || account.getAsset().getCategory() != AssetCategory.CASH) {
                continue;
            }
            BigDecimal balance = transactionRepository.getAccountBalance(account.getId());
            liquidCash = liquidCash.add(balance.multiply(rate(account, targetAsset, financialOracle, rateCache)));
        }
        BigDecimal runway = burn != null && burn.signum() > 0
                ? liquidCash.max(BigDecimal.ZERO).divide(burn, 4, RoundingMode.HALF_UP)
                : null;

        return new CashflowReport(income, expenses, net, savingsRate, months, burn, liquidCash, runway,
                symbol, period, incomeLines, expenseLines);
    }

    /**
     * Net flow per leaf account of {@code type} in the period, converted to the target asset and sorted largest first.
     * INCOME flows are Σfrom − Σto; with {@code negate} the sign flips to Σto − Σfrom, the EXPENSE balance rule.
     */
    private List<CashflowReport.FlowLine> flowLines(AccountType type, boolean negate, ReportPeriod period,
                                                    Asset targetAsset, FinancialOracle oracle,
                                                    Map<Long, BigDecimal> rateCache) {
        List<CashflowReport.FlowLine> lines = new ArrayList<>();
        for (Object[] row : transactionRepository.sumNetOutflowByAccountType(
                type, period.startMillis(), period.endExclusiveMillis())) {
            Account account = (Account) row[0];
            BigDecimal net = toBigDecimal(row[1]);
            if (negate) {
                net = net.negate();
            }
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

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
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
