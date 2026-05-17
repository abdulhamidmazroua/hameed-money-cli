package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.enums.AccountType;
import org.hameed.hameedmoneycli.model.dto.NetworthReport;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.hameed.hameedmoneycli.model.entity.Asset;
import org.hameed.hameedmoneycli.model.entity.FinancialOracle;
import org.hameed.hameedmoneycli.repository.AccountRepository;
import org.hameed.hameedmoneycli.repository.AssetRepository;
import org.hameed.hameedmoneycli.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {


    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AssetRepository assetRepository;
    private final MarketQuoteService marketQuoteService;

    public NetworthReport generateNetworthReport(String currency) {
        FinancialOracle financialOracle = marketQuoteService.getFinancialOracle();
        Asset targetAsset = assetRepository.findBySymbol(currency)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + currency));

        Map<Long, BigDecimal> rateCache = new HashMap<>();

        BigDecimal totalAssets = accountRepository.getLeafAccounts().stream()
                .filter(account -> account.getMasterType() == AccountType.ASSET ||
                        account.getMasterType() == AccountType.LIABILITY)
                .map(account -> {
                    BigDecimal balance = transactionRepository.getAccountBalance(account.getId());
                    BigDecimal rate = rateCache.computeIfAbsent(
                            account.getAsset().getId(),
                            key -> financialOracle.getRate(key, targetAsset.getId())
                    );
                    return balance.multiply(rate);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiabilities = accountRepository.getLeafAccounts().stream()
                .filter(account -> account.getMasterType() == AccountType.LIABILITY)
                .map(account -> {
                    BigDecimal balance = transactionRepository.getAccountBalance(account.getId());
                    BigDecimal rate = rateCache.computeIfAbsent(
                            account.getAsset().getId(),
                            key -> financialOracle.getRate(key, targetAsset.getId())
                    );
                    return balance.multiply(rate);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new NetworthReport(
                totalAssets,
                totalLiabilities,
                totalAssets.subtract(totalLiabilities)
        );
    }
}
