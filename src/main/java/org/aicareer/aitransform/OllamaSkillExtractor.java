package org.aicareer.aitransform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class OllamaSkillExtractor {

    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private static final String MODEL_NAME = "llama3";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String[] DEFAULT_SKILLS = {
            "java", "c++", "python", "javascript", "sql",
            "docker", "c#", "php", "spring", "machine_learning"
    };

    public VacancySkills extract(String vacancyJson) throws IOException, InterruptedException {

        String systemPrompt = buildSystemPrompt();

        // Build JSON request body
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", MODEL_NAME);
        payload.put("stream", false);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", vacancyJson));
        payload.put("messages", messages);

        String requestBody = MAPPER.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = MAPPER.readTree(response.body());
        String messageContent = root.path("message").path("content").asText().trim();

        messageContent = stripCodeFence(messageContent);

        JsonNode skillsJson = MAPPER.readTree(messageContent).path("skills");

        Map<String, Integer> result = new HashMap<>();
        for (String skill : DEFAULT_SKILLS) {
            int value = skillsJson.path(skill).asInt(0);
            result.put(skill, value);
        }

        return new VacancySkills(result);
    }

    // --- LOAD PROMPT FROM FILE ---

    private String buildSystemPrompt() throws IOException {
        String template = loadPromptTemplate();

        String skillsList = Arrays.stream(DEFAULT_SKILLS)
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(", ", "[", "]"));

        return template.replace("{{SKILLS}}", skillsList);
    }

    private String loadPromptTemplate() throws IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("prompts/skill_extraction_prompt.txt");

        if (is == null) {
            throw new IOException("Prompt file not found: prompts/skill_extraction_prompt.txt");
        }

        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String stripCodeFence(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int idx = t.indexOf('\n');
            if (idx > 0) t = t.substring(idx + 1);
        }
        if (t.endsWith("```")) {
            t = t.substring(0, t.lastIndexOf("```"));
        }
        return t.trim();
    }
}
