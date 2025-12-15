package com.aicareer.aitransform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight client for sending prompts to the OpenAI chat completion API.
 */
public class OpenAIClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = System.getenv().getOrDefault(
            "OPENAI_MODEL",
            "gpt-4o-mini"
    );

    private static final int MAX_RETRIES_ON_419 = 2;
    private static final long RETRY_DELAY_SECONDS = 10_000;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String generate(String model, String prompt) {
        return generate(model, prompt, null);
    }

    public String generate(String model, String prompt, Path imagePath) {
        String apiKey = Config.getApiKey();

        String resolvedModel = (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim();

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", resolvedModel);
        payload.put("max_tokens", 2048);
        payload.put("messages", List.of(Map.of(
                "role", "user",
                "content", buildContent(prompt, imagePath)
        )));

        try {
            String body = MAPPER.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("OpenAI-Beta", "assistants=v2")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            for (int attempt = 0; attempt <= MAX_RETRIES_ON_419; attempt++) {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 419 && attempt < MAX_RETRIES_ON_419) {
                    System.err.println("[AI] Получен 419, повторяем запрос через "
                            + RETRY_DELAY_SECONDS + " секунд (попытка " + (attempt + 2) + "/"
                            + (MAX_RETRIES_ON_419 + 1) + ")");
                    TimeUnit.SECONDS.sleep(RETRY_DELAY_SECONDS);
                    continue;
                }

                if (response.statusCode() >= 300) {
                    throw new IllegalStateException("Model call failed: HTTP " + response.statusCode()
                            + " -> " + response.body());
                }

                JsonNode json = MAPPER.readTree(response.body());
                JsonNode choices = json.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    JsonNode content = choices.get(0).path("message").path("content");
                    if (!content.isMissingNode()) {
                        return content.asText();
                    }
                }
                return response.body();
            }

            throw new IllegalStateException("Model call failed after retries with 419 errors");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Model call was interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call OpenAI model", e);
        }
    }

    private Object buildContent(String prompt, Path imagePath) {
        if (imagePath == null) {
            return prompt;
        }

        return List.of(
                Map.of(
                        "type", "text",
                        "text", prompt
                ),
                Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", encodeImage(imagePath))
                )
        );
    }

    private String encodeImage(Path imagePath) {
        if (!Files.exists(imagePath)) {
            throw new IllegalStateException("Skill graph image not found: " + imagePath.toAbsolutePath());
        }

        try {
            byte[] bytes = Files.readAllBytes(imagePath);
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "data:image/png;base64," + base64;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load skill graph image for the model", e);
        }
    }
}

