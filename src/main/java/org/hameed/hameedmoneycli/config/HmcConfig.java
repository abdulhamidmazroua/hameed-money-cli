package org.hameed.hameedmoneycli.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class HmcConfig {

    private static final Logger log = LoggerFactory.getLogger(HmcConfig.class);

    private final Path configPath;
    private final ObjectMapper objectMapper;
    private volatile ConfigData cached;

    public HmcConfig(
            @Value("${hmc.config.path}") String configPath,
            ObjectMapper objectMapper
    ) {
        this.configPath = Path.of(configPath);
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        try {
            if (Files.notExists(configPath)) {
                writeDefault();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init config at " + configPath, e);
        }
    }

    private ConfigData data() {
        ConfigData local = cached;
        if (local != null) return local;
        synchronized (this) {
            if (cached == null) cached = load();
            return cached;
        }
    }

    private ConfigData load() {
        try {
            return objectMapper.readValue(configPath.toFile(), ConfigData.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + configPath, e);
        }
    }

    private void writeDefault() {
        try {
            Files.createDirectories(configPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), ConfigData.DEFAULT);
            log.info("Created default config at {}", configPath);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create default config at " + configPath, e);
        }
    }

    public String getMarketDataProvider() {
        String p = data().marketDataProvider;
        return p != null ? p : "eodhd";
    }

    public String requireEodhdApiKey() {
        var e = data().eodhd;
        if (e == null || e.apiKey == null || e.apiKey.isBlank()) {
            throw new IllegalStateException(
                    "EODHD API key not found. Add \"eodhd\": { \"apiKey\": \"YOUR_KEY\" } to " + configPath
            );
        }
        return e.apiKey;
    }

    public String requireTwelveDataApiKey() {
        var t = data().twelveData;
        if (t == null || t.apiKey == null || t.apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Twelve Data API key not found. Add \"twelveData\": { \"apiKey\": \"YOUR_KEY\" } to " + configPath
            );
        }
        return t.apiKey;
    }

    public LlmConfig getLlmConfig() {
        return data().llm;
    }

    public record LlmConfig(
            @JsonProperty("provider") String provider,
            @JsonProperty("model") String model,
            @JsonProperty("baseUrl") String baseUrl,
            @JsonProperty("apiKey") String apiKey,
            @JsonProperty("classifyPrompt") String classifyPrompt
    ) {}

    private record ConfigData(
            @JsonProperty("marketDataProvider") String marketDataProvider,
            @JsonProperty("eodhd") EodhdConfig eodhd,
            @JsonProperty("twelveData") TwelveDataConfig twelveData,
            @JsonProperty("llm") LlmConfig llm
    ) {
        static final ConfigData DEFAULT = new ConfigData("eodhd", new EodhdConfig(""), new TwelveDataConfig(""), null);

        private record EodhdConfig(@JsonProperty("apiKey") String apiKey) {}
        private record TwelveDataConfig(@JsonProperty("apiKey") String apiKey) {}
    }
}
