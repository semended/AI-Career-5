package com.aicareer.aitransform;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TryQwen {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static void main(String[] args) throws Exception {

    String apiKey = Config.API_KEY;
    System.out.println("Using API Key: " + apiKey);
    URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
    HttpURLConnection con = (HttpURLConnection) url.openConnection();

    con.setRequestMethod("POST");
    con.setRequestProperty("Authorization", "Bearer " + apiKey);
    con.setRequestProperty("Content-Type", "application/json");
    con.setRequestProperty("HTTP-Referer", "https://example.com");
    con.setRequestProperty("X-Title", "AI-Career-5");

    con.setDoOutput(true);

    String body = """
        {
          "model": "qwen/qwen3-4b:free",
          "max_tokens": 512,
          "messages": [
            { "role": "user", "content": "Hello! How are you?" }
          ]
        }
        """;

    try (OutputStream os = con.getOutputStream()) {
      os.write(body.getBytes());
    }

    int status = con.getResponseCode();
    System.out.println("HTTP status: " + status);

    InputStream is = (status >= 200 && status < 300)
        ? con.getInputStream()
        : con.getErrorStream();

    StringBuilder sb = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
    }

    String json = sb.toString();
    System.out.println("RAW JSON:\n" + json + "\n");

    JsonNode root = MAPPER.readTree(json);
    String content = root.path("choices")
        .get(0)
        .path("message")
        .path("content")
        .asText();

    System.out.println("ASSISTANT:\n" + content);
  }
}
