package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AccountType;
import org.hameed.hameedmoneycli.enums.AssetCategory;
import org.hameed.hameedmoneycli.model.dto.DataIntegrityReport;
import org.hameed.hameedmoneycli.model.dto.NetworthReport;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.model.entity.FinancialOracle;
import org.hameed.hameedmoneycli.model.entity.Transaction;
import org.hameed.hameedmoneycli.repository.AccountRepository;
import org.hameed.hameedmoneycli.repository.AssetRepository;
import org.hameed.hameedmoneycli.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
