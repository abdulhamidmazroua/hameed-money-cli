package org.hameed.hameedmoneycli.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${hmc.market.data.provider.twelve-date.base-url}")
    private String twelveDataBaseUrl;
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(twelveDataBaseUrl)
                .build();
    }
}
