package com.badminton.shop.modules.chatbot.service.impl;

import com.badminton.shop.modules.chatbot.service.GeminiClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiClientServiceImpl implements GeminiClientService {

    private static final String FIXED_API_VERSION = "v1beta";
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 1024;
    private static final int CONTINUATION_MAX_OUTPUT_TOKENS = 1536;
    private static final int MIN_REASONABLE_ANSWER_LENGTH = 220;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;

    @Value("${GEMINI_TOKEN:}")
    private String geminiToken;

    @Value("${app.chatbot.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${app.chatbot.gemini.enable-continuation:false}")
    private boolean continuationEnabled;

    public GeminiClientServiceImpl(
            @Value("${app.chatbot.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${app.chatbot.gemini.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${app.chatbot.gemini.read-timeout-ms:12000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String generateAnswer(String prompt) {
        if (geminiToken == null || geminiToken.isBlank()) {
            throw new IllegalStateException("Missing GEMINI_TOKEN environment variable.");
        }
        try {
            GeminiParseResult initialResult = callGemini(prompt, DEFAULT_MAX_OUTPUT_TOKENS);
            String answer = initialResult.text();

            if (isSafetyStopped(initialResult.finishReasons())) {
                throw new IllegalStateException("Gemini response blocked by safety filters.");
            }

            if (continuationEnabled && shouldRetryAsContinuation(initialResult)) {
                String continuationPrompt = buildContinuationPrompt(prompt, answer);
                GeminiParseResult continuationResult = callGemini(continuationPrompt, CONTINUATION_MAX_OUTPUT_TOKENS);
                if (isSafetyStopped(continuationResult.finishReasons())) {
                    throw new IllegalStateException("Gemini continuation blocked by safety filters.");
                }
                String continuationText = continuationResult.text();
                if (!continuationText.isBlank()) {
                    answer = sanitizeText(answer + "\n" + continuationText);
                }
            }

            log.info("Gemini selected model={} apiVersion={}", geminiModel, FIXED_API_VERSION);
            return answer;
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Gemini call failed: " + ex.getResponseBodyAsString(), ex);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Gemini call failed.", ex);
        }
    }

    private GeminiParseResult callGemini(String prompt, int maxOutputTokens) {
        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "topP", 0.9,
                        "maxOutputTokens", maxOutputTokens,
                        "responseMimeType", "text/plain"
                )
        );

        String responseBody = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/{apiVersion}/models/{model}:generateContent")
                        .queryParam("key", geminiToken)
                    .build(FIXED_API_VERSION, geminiModel))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        return extractText(responseBody);
    }

    private GeminiParseResult extractText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Gemini returned empty response.");
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new IllegalStateException("Gemini response does not contain candidates.");
            }

            List<String> candidateTexts = new ArrayList<>();
            List<String> finishReasons = new ArrayList<>();

            for (JsonNode candidate : candidates) {
                finishReasons.add(candidate.path("finishReason").asText("UNKNOWN"));

                JsonNode parts = candidate.path("content").path("parts");
                if (!parts.isArray() || parts.isEmpty()) {
                    continue;
                }

                StringBuilder textBuilder = new StringBuilder();
                for (JsonNode part : parts) {
                    String partText = part.path("text").asText("").trim();
                    if (partText.isBlank()) {
                        continue;
                    }
                    if (!textBuilder.isEmpty()) {
                        textBuilder.append("\n");
                    }
                    textBuilder.append(partText);
                }

                String normalized = sanitizeText(textBuilder.toString());
                if (!normalized.isBlank()) {
                    candidateTexts.add(normalized);
                }
            }

            String bestText = candidateTexts.stream()
                    .max((a, b) -> Integer.compare(a.length(), b.length()))
                    .orElse("")
                    .trim();

            if (!bestText.isBlank()) {
                log.info("Gemini extracted answer length={} finishReasons={}", bestText.length(), finishReasons);
                return new GeminiParseResult(bestText, finishReasons);
            }

            throw new IllegalStateException("Gemini response does not contain text output.");
        } catch (Exception ex) {
            log.error("Failed to parse Gemini response", ex);
            throw new IllegalStateException("Cannot parse Gemini response.");
        }
    }

    private String sanitizeText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == '\t' || !Character.isISOControl(ch)) {
                sb.append(ch);
            }
        }
        return sb.toString().trim();
    }

    private boolean shouldRetryAsContinuation(GeminiParseResult result) {
        return result.text().length() < MIN_REASONABLE_ANSWER_LENGTH
                && result.finishReasons().stream().anyMatch("MAX_TOKENS"::equalsIgnoreCase);
    }

    private boolean isSafetyStopped(List<String> finishReasons) {
        return finishReasons.stream().anyMatch(reason -> "SAFETY".equalsIgnoreCase(reason)
                || "RECITATION".equalsIgnoreCase(reason));
    }

    private String buildContinuationPrompt(String originalPrompt, String partialAnswer) {
        return "Bạn đang trả lời dang dở vì giới hạn độ dài. "
                + "Hãy tiếp tục từ đúng ý cuối cùng, không lặp lại phần đã viết, vẫn bằng tiếng Việt.\n\n"
                + "Câu hỏi ban đầu:\n" + originalPrompt + "\n\n"
                + "Phần đã trả lời:\n" + partialAnswer;
    }

    private record GeminiParseResult(String text, List<String> finishReasons) {
    }
}
