package org.hameed.hameedmoneycli.service;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.config.HmcConfig;
import org.hameed.hameedmoneycli.constants.PromptConstants;
import org.hameed.hameedmoneycli.proxy.LlmProxy;
import org.hameed.hameedmoneycli.proxy.TextExtractorProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConvertService {

    private static final Logger log = LoggerFactory.getLogger(ConvertService.class);

    private final HmcConfig hmcConfig;
    private final TextExtractorProxy textExtractor;
    private final LlmProxy llmProxy;

    public String convertToCsv(String inputPath) {
        HmcConfig.LlmConfig llm = hmcConfig.getLlmConfig();
        if (llm == null || llm.provider() == null) {
            throw new IllegalStateException("LLM not configured. Add an \"llm\" section to config.json.");
        }

        String extractedText = textExtractor.extract(inputPath)
                .orElseThrow(() -> new RuntimeException(
                        "Failed to extract text from " + inputPath
                        + ". Ensure Python dependencies are installed: pip install pypdf pytesseract pdf2image openpyxl Pillow"));

        String prompt = String.format(PromptConstants.CONVERT_PROMPT, extractedText);

        log.info("Calling LLM ({}) to convert extracted text...", llm.provider());
        String response = llmProxy.call(prompt, llm);

        if (response == null || response.isBlank()) {
            throw new RuntimeException("LLM returned empty response");
        }

        return stripCodeFences(response.trim());
    }

    private String stripCodeFences(String csv) {
        if (csv.startsWith("```csv")) csv = csv.substring(6);
        else if (csv.startsWith("```")) csv = csv.substring(3);
        if (csv.endsWith("```")) csv = csv.substring(0, csv.length() - 3);
        return csv.trim();
    }
}
