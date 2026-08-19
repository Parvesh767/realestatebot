package com.risingbee.realestate.automation.parser;


import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiQueryParser {

    private final RestClient restClient;

    public AiQueryParser(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.openai.com/v1").build();
    }

    public String extractStructuredJson(String userMessage) {
        String prompt = """
            Extract real estate criteria from this message: "%s".
            Return raw JSON with keys: bhk (Integer), location (String), maxBudget (Number in INR).
            """.formatted(userMessage);

        // Call LLM endpoint returning structured JSON payload
        return restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
                .body(Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(Map.of("role", "user", "content", prompt))
                ))
                .retrieve()
                .body(String.class);
    }
}