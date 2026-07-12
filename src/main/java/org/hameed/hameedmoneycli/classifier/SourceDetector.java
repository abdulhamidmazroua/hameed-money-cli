package org.hameed.hameedmoneycli.classifier;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.config.HmcConfig;
import org.hameed.hameedmoneycli.model.entity.SourceFormatConfig;
import org.hameed.hameedmoneycli.constants.PromptConstants;
import org.hameed.hameedmoneycli.proxy.LlmProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SourceDetector {

    private static final Logger log = LoggerFactory.getLogger(SourceDetector.class);

    private final HmcConfig hmcConfig;
    private final LlmProxy llmProxy;
    private final ObjectMapper objectMapper;

    public DetectedFormat detect(String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("File is empty: " + filePath);
        }

        int sampleSize = Math.min(20, lines.size());
        List<String> sample = lines.subList(0, sampleSize);
        String sampleText = sample.stream().collect(Collectors.joining("\n"));

        HmcConfig.LlmConfig llm = hmcConfig.getLlmConfig();
        if (llm == null || llm.provider() == null) {
            return new DetectedFormat(null, null, "LLM not configured; run --manual to define format");
        }

        String prompt = String.format(PromptConstants.SOURCE_DETECT_PROMPT, sampleText);
        String response;
        try {
            response = llmProxy.call(prompt, llm);
        } catch (Exception e) {
            return new DetectedFormat(null, "LLM call failed: " + e.getMessage(), null);
        }

        return parseResponse(response, sample);
    }

    private DetectedFormat parseResponse(String response, List<String> sample) {
        if (response == null || response.isBlank()) {
            return new DetectedFormat(null, "LLM returned empty response", null);
        }

        try {
            String json = response.trim();
            if (json.startsWith("```json")) json = json.substring(7);
            if (json.startsWith("```")) json = json.substring(3);
            if (json.endsWith("```")) json = json.substring(0, json.length() - 3);
            json = json.trim();

            int braceStart = json.indexOf('{');
            int braceEnd = json.lastIndexOf('}');
            if (braceStart >= 0 && braceEnd > braceStart) {
                json = json.substring(braceStart, braceEnd + 1);
            }

            SourceFormatConfig config = objectMapper.readValue(json, SourceFormatConfig.class);
            return new DetectedFormat(config, null, null);
        } catch (Exception e) {
            return new DetectedFormat(null, "Failed to parse LLM response: " + e.getMessage(), response);
        }
    }

    public record DetectedFormat(SourceFormatConfig config, String error, String rawResponse) {}
}
