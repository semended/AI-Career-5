package com.aicareer.aitransform;

/**
 * Centralized configuration holder for secrets and runtime settings.
 */
public final class Config {
    /**
     * API key for OpenRouter-based model calls, taken from the run configuration
     * (environment variable or JVM property OPENROUTER_API_KEY).
     */
    public static final String API_KEY = resolveApiKey();

    private Config() {
    }

    private static String resolveApiKey() {
        String key = System.getenv("OPENROUTER_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getProperty("OPENROUTER_API_KEY", "");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("OPENROUTER_API_KEY is not configured");
        }
        return key;
    }
}
