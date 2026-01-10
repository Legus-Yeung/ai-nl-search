package com.example.ai_nl_search.service;

import com.example.ai_nl_search.dto.OrderFilter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BedrockNlService {

    private static final String MODEL_ID = "openai.gpt-oss-20b-1:0";
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}", Pattern.DOTALL);

    private final BedrockRuntimeClient client;
    private final ObjectMapper objectMapper;

    public BedrockNlService() {
        this.client = BedrockRuntimeClient.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public OrderFilter interpret(String naturalLanguageQuery) {
        try {
            String requestBody = buildFilterRequest(naturalLanguageQuery);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(MODEL_ID)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(requestBody))
                    .build();

            InvokeModelResponse response = client.invokeModel(request);

            String responseJson = response.body().asUtf8String();

            String textResponse = extractTextResponse(responseJson);
            
            OrderFilter filter = extractJsonFromText(textResponse);
            
            return filter;

        } catch (Exception e) {
            throw new RuntimeException("Failed to interpret natural language query", e);
        }
    }

    private String buildFilterRequest(String userQuery) {
        return """
        {
          "max_tokens": 512,
          "temperature": 0,
          "messages": [
            {
              "role": "system",
              "content": [
                {
                  "type": "text",
                  "text": "You are a system that converts natural language order search queries into structured JSON filters. You must respond with ONLY a valid JSON object matching this exact schema:\\n{\\n  \\"location_name\\": string or null,\\n  \\"date_from\\": string in YYYY-MM-DD format or null,\\n  \\"date_to\\": string in YYYY-MM-DD format or null,\\n  \\"exclude_status\\": array of strings (only \\"EXPIRED\\" is allowed) or null,\\n  \\"collected_by\\": array of strings (only \\"COURIER\\", \\"CUSTOMER\\", \\"OPERATOR\\" are allowed) or null\\n}\\n\\nDo not include any explanation, reasoning, or text outside the JSON. Return ONLY the JSON object."
                }
              ]
            },
            {
              "role": "user",
              "content": [
                {
                  "type": "text",
                  "text": "%s"
                }
              ]
            }
          ]
        }
        """.formatted(userQuery.replace("\"", "\\\"").replace("\n", "\\n"));
    }

    private String extractTextResponse(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.path("message");
            
            JsonNode content = message.path("content");
            if (content.isTextual()) {
                return content.asText();
            }
            
            if (content.isArray() && content.size() > 0) {
                JsonNode textContent = content.get(0);
                JsonNode text = textContent.path("text");
                if (!text.isMissingNode()) {
                    return text.asText();
                }
            }
        }

        throw new IllegalStateException("Failed to extract text response from Bedrock response");
    }

    private OrderFilter extractJsonFromText(String textResponse) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(textResponse);
            return objectMapper.treeToValue(jsonNode, OrderFilter.class);
        } catch (Exception e) {
            Matcher matcher = JSON_PATTERN.matcher(textResponse);
            if (matcher.find()) {
                String jsonStr = matcher.group(0);
                JsonNode jsonNode = objectMapper.readTree(jsonStr);
                return objectMapper.treeToValue(jsonNode, OrderFilter.class);
            }
            
            Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
            Matcher codeBlockMatcher = codeBlockPattern.matcher(textResponse);
            if (codeBlockMatcher.find()) {
                String jsonStr = codeBlockMatcher.group(1);
                JsonNode jsonNode = objectMapper.readTree(jsonStr);
                return objectMapper.treeToValue(jsonNode, OrderFilter.class);
            }
            
            throw new IllegalStateException("Could not extract JSON from response: " + textResponse);
        }
    }
}
