package org.hameed.hameedmoneycli.proxy;

import org.hameed.hameedmoneycli.config.HmcConfig;
import org.hameed.hameedmoneycli.proxy.dto.TwelveDataListContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class TwelveDataProxy {

    private final HmcConfig config;
    private final String stocksEndpoint;
    private final RestClient restClient;

    public TwelveDataProxy(
            HmcConfig config,
            @Value("${hmc.market.data.provider.twelve-data.endpoint.stocks}") String stocksEndpoint,
            RestClient restClient
    ) {
        this.config = config;
        this.stocksEndpoint = stocksEndpoint;
        this.restClient = restClient;
    }

    public List<Map<String, String>> getExchangeSymbols(String exchange) {
        String stocksUri = stocksEndpoint + "?apiKey=" + config.requireTwelveDataApiKey() + "&exchange=" + exchange;
        TwelveDataListContainer<Map<String, String>> twelveDataListContainer = restClient.get()
                .uri(stocksUri)
                .retrieve()
                .body(TwelveDataListContainer.class);
        return twelveDataListContainer.data();
    }
}
