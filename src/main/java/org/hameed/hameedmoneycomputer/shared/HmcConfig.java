package org.hameed.hameedmoneycomputer.shared;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

/**
 * User configuration in {@code config.json}: base currency, timezone, provider selection and API keys.
 * Nothing here has a regional default: the base currency is chosen at {@code setup} and the timezone
 * falls back to the system timezone.
 */
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
        if (Files.notExists(configPath)) {
            write(ConfigData.DEFAULT);
            log.info("Created default config at {}", configPath);
        }
        timezone().ifPresent(DateUtil::useZone);
    }

    /** The ledger's base currency (ISO 4217 code), chosen at {@code setup}. Empty until then. */
    public Optional<String> baseCurrency() {
        String c = data().baseCurrency;
        return c == null || c.isBlank() ? Optional.empty() : Optional.of(c.trim().toUpperCase(Locale.ROOT));
    }

    public String requireBaseCurrency() {
        return baseCurrency().orElseThrow(() -> new BusinessRuleException(
                "No base currency set. Run `setup --currency <CUR>` first (e.g. setup --currency USD)."));
    }

    public synchronized void setBaseCurrency(String currency) {
        ConfigData d = data();
        write(new ConfigData(currency, d.timezone, d.marketDataProvider, d.eodhd, d.twelveData, d.llm));
    }

    /** IANA timezone from {@code "timezone"} in config.json; empty means the system timezone. */
    public Optional<ZoneId> timezone() {
        String z = data().timezone;
        if (z == null || z.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ZoneId.of(z.trim()));
        } catch (DateTimeException e) {
            throw new ValidationException("Invalid \"timezone\" '" + z + "' in " + configPath
                    + ". Use an IANA id such as Europe/Berlin, or remove it to use the system timezone.");
        }
    }

    public String getMarketDataProvider() {
        String p = data().marketDataProvider;
        return p != null ? p : "eodhd";
    }

    public String requireEodhdApiKey() {
        var e = data().eodhd;
        if (e == null || e.apiKey == null || e.apiKey.isBlank()) {
            throw new BusinessRuleException(
                    "EODHD API key not found. Add \"eodhd\": { \"apiKey\": \"YOUR_KEY\" } to " + configPath
            );
        }
        return e.apiKey;
    }

    public String requireTwelveDataApiKey() {
        var t = data().twelveData;
        if (t == null || t.apiKey == null || t.apiKey.isBlank()) {
            throw new BusinessRuleException(
                    "Twelve Data API key not found. Add \"twelveData\": { \"apiKey\": \"YOUR_KEY\" } to " + configPath
            );
        }
        return t.apiKey;
    }

    public LlmConfig getLlmConfig() {
        return data().llm;
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

    private synchronized void write(ConfigData data) {
        try {
            Files.createDirectories(configPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), data);
            cached = data;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write config " + configPath, e);
        }
    }

    public record LlmConfig(
            @JsonProperty("provider") String provider,
            @JsonProperty("model") String model,
            @JsonProperty("baseUrl") String baseUrl,
            @JsonProperty("apiKey") String apiKey,
            @JsonProperty("classifyPrompt") String classifyPrompt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ConfigData(
            @JsonProperty("baseCurrency") String baseCurrency,
            @JsonProperty("timezone") String timezone,
            @JsonProperty("marketDataProvider") String marketDataProvider,
            @JsonProperty("eodhd") EodhdConfig eodhd,
            @JsonProperty("twelveData") TwelveDataConfig twelveData,
            @JsonProperty("llm") LlmConfig llm
    ) {
        static final ConfigData DEFAULT = new ConfigData(null, null, "eodhd", new EodhdConfig(""), new TwelveDataConfig(""), null);

        private record EodhdConfig(@JsonProperty("apiKey") String apiKey) {}
        private record TwelveDataConfig(@JsonProperty("apiKey") String apiKey) {}
    }
}
