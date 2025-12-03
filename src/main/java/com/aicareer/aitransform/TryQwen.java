package com.aicareer.aitransform;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class TryQwen {
  public static void main(String[] args) throws Exception {
    String apiKey = "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";
    System.out.println(apiKey);
    URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
    HttpURLConnection con = (HttpURLConnection) url.openConnection();

    con.setRequestMethod("POST");
    con.setRequestProperty("Authorization", "Bearer " + apiKey);
    con.setRequestProperty("Content-Type", "application/json");

    // не обязательно, но можно
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

    int code = con.getResponseCode();
    System.out.println("HTTP status: " + code);

    InputStream is = (code >= 200 && code < 300)
        ? con.getInputStream()
        : con.getErrorStream();

    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String line;
      StringBuilder sb = new StringBuilder();
      while ((line = br.readLine()) != null) {
        sb.append(line).append('\n');
      }
      System.out.println("Response body:");
      System.out.println(sb);
    }
  }
}
