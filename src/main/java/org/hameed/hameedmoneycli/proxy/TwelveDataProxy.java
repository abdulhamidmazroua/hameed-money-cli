package org.hameed.hameedmoneycli.proxy;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.proxy.dto.TwelveDataListContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TwelveDataProxy {

    @Value("${hmc.market.data.provider.twelve-data.api-key}")
    private String apiKey;

    @Value("${hmc.market.data.provider.twelve-data.endpoint.stocks}")
    private String stocksEndpoint;

    private final RestClient restClient;

    public List<Map<String, String>> getExchangeSymbols(String exchange) {
        String stocksUri = stocksEndpoint + "?apiKey=" +  apiKey + "&exchange=" + exchange;
        TwelveDataListContainer<Map<String, String>> twelveDataListContainer = restClient.get()
                .uri(stocksUri)
                .retrieve()
                .body(TwelveDataListContainer.class);
        return twelveDataListContainer.data();
    }
}
