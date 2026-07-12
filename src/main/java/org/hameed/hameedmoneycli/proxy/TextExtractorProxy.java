package org.hameed.hameedmoneycli.proxy;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class TextExtractorProxy {

    private static final String SCRIPT_PATH = "scripts/extract_text.py";
    private static final long TIMEOUT_SECONDS = 60;

    private final ObjectMapper objectMapper;

    public TextExtractorProxy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<String> extract(String filePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", SCRIPT_PATH, filePath);
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
            if (root.has("error") && !root.get("error").isNull()) {
                return Optional.empty();
            }

            JsonNode textNode = root.get("text");
            return textNode != null ? Optional.of(textNode.asText()) : Optional.empty();

        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
