package org.hameed.hameedmoneycli.config;

import org.hameed.hameedmoneycli.util.IngestionStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class IngestionStrategyConfiguration {

    @Bean
    public Map<String, IngestionStrategy> ingestionStrategyMap(List<IngestionStrategy> strategies) {
        return strategies.stream()
                .collect(Collectors.toMap(
                        s -> s.supportedSource().name(),
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(
                                    "Duplicate ingestion strategy registration: " + a.supportedSource());
                        }));
    }
}
