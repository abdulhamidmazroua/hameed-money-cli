package org.hameed.hameedmoneycli.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class YahooFinanceProxy {

    private static final String SCRIPT_PATH = "scripts/yahoo_price.py";
    private static final long TIMEOUT_SECONDS = 15;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<BigDecimal> fetchPrice(String yahooSymbol) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", SCRIPT_PATH, yahooSymbol);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Optional.empty();
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(output.toString());
            if (root.has("error")) {
                return Optional.empty();
            }

            JsonNode priceNode = root.get("price");
            return priceNode != null ? Optional.of(priceNode.decimalValue()) : Optional.empty();

        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
