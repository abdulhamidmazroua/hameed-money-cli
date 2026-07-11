package org.hameed.hameedmoneycli.proxy;

import org.hameed.hameedmoneycli.config.HmcConfig;
import org.hameed.hameedmoneycli.proxy.dto.EodhdSymbolDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class EodhdProxy {

    private final HmcConfig config;
    private final String exchangeSymbolListEndpoint;
    private final RestClient restClient;

    public EodhdProxy(
            HmcConfig config,
            @Value("${hmc.market.data.provider.eodhd.endpoint.exchange-symbol-list}") String exchangeSymbolListEndpoint
    ) {
        this.config = config;
        this.exchangeSymbolListEndpoint = exchangeSymbolListEndpoint;
        this.restClient = RestClient.create();
    }

    public List<EodhdSymbolDto> getExchangeSymbols(String exchange) {
        String uri = exchangeSymbolListEndpoint + "/" + exchange + "?api_token=" + config.requireEodhdApiKey() + "&fmt=json";
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
