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
      String systemPrompt = """
        You are an agent that converts natural language order search queries into structured JSON filters.
        
        DATABASE CONTEXT:
        - Valid order STATUSES: CREATED, COURIER_STORED, CUSTOMER_STORED, DELIVERED, OPERATOR_COLLECTED, EXPIRED
        - Valid SERVICE types: DELIVERY, RETURNS
        - Valid COLLECTED_BY types: COURIER, CUSTOMER, OPERATOR
        - Valid LOCATION_TYPES: PUDO, LOCKER, WAREHOUSE, STORE
        - Sample LOCATIONS: "alfred24 Office Locker", "Location A", "Location B", "7Eleven Kwai Chung - PUDO", "ABI Graphique Demo Locker Normal", "ABI Graphique Demo Locker Temp Control"
        - Sample CITIES: "Hong Kong", "Paris"
        - Sample COMPANIES: "Demo Company", "7Eleven Kwai Chung", "ABI Graphique Demo"
        - Sample CARRIERS: "DHL", "SF Express", "UPS"
        
        JSON SCHEMA (all fields are optional; use null if not specified):
        {
          "location_name": string or null (partial match),
          "location_type": string or null (exact match: PUDO, LOCKER, WAREHOUSE, STORE),
          "date_from": string in YYYY-MM-DD format or null (filters time_created),
          "date_to": string in YYYY-MM-DD format or null (filters time_created),
          "exclude_status": array of strings or null (valid: CREATED, COURIER_STORED, CUSTOMER_STORED, DELIVERED, OPERATOR_COLLECTED, EXPIRED),
          "collected_by": array of strings or null (valid: COURIER, CUSTOMER, OPERATOR),
          "service": array of strings or null (valid: DELIVERY, RETURNS),
          "city": string or null (exact match),
          "company_name": string or null (partial match),
          "carrier_name": string or null (partial match)
        }
        
        INTERPRETATION RULES:
        - "exclude expired" / "not expired" / "excluding expired" → exclude_status: ["EXPIRED"]
        - "show expired orders" / "all expired" → exclude_status: ["CREATED","COURIER_STORED","CUSTOMER_STORED","DELIVERED","OPERATOR_COLLECTED"]
        - "delivered orders" → exclude_status: ["CREATED","COURIER_STORED","CUSTOMER_STORED","OPERATOR_COLLECTED","EXPIRED"]
        - Mention of location name → location_name (partial match)
        - Mention of location type ("locker", "pudo", "warehouse", "store") → location_type (exact match)
        - Mention of company name → company_name (partial match)
        - Mention of carrier name → carrier_name (partial match)
        - Mention of city → city (exact match)
        - Mention of service type ("delivery" / "returns") → service
        - Mention of collected by type → collected_by
        - Mention of date range → date_from and/or date_to
        - Only include fields that can be explicitly inferred; use null for all unspecified fields
        
        EXAMPLES:
        - "Show me all orders excluding expired" →
          {"exclude_status":["EXPIRED"],"location_name":null,"location_type":null,"date_from":null,"date_to":null,"collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null}
        
        - "Show expired orders at Location A" →
          {"exclude_status":["CREATED","COURIER_STORED","CUSTOMER_STORED","DELIVERED","OPERATOR_COLLECTED"],"location_name":"Location A","location_type":null,"date_from":null,"date_to":null,"collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null}
        
        - "Delivered orders at ABI Graphique Demo Locker Normal" →
          {"exclude_status":["CREATED","COURIER_STORED","CUSTOMER_STORED","OPERATOR_COLLECTED","EXPIRED"],"location_name":"ABI Graphique Demo Locker Normal","location_type":"LOCKER","date_from":null,"date_to":null,"collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null}
        
        - "All DHL deliveries at lockers in Hong Kong from January 2026" →
          {"exclude_status":null,"location_name":null,"location_type":"LOCKER","date_from":"2026-01-01","date_to":"2026-01-31","collected_by":null,"service":["DELIVERY"],"city":"Hong Kong","company_name":null,"carrier_name":"DHL"}
        
        - "Orders from January 2026 excluding expired for 7Eleven Kwai Chung" →
          {"exclude_status":["EXPIRED"],"location_name":null,"location_type":null,"date_from":"2026-01-01","date_to":"2026-01-31","collected_by":null,"service":null,"city":null,"company_name":"7Eleven Kwai Chung","carrier_name":null}
        
        INSTRUCTIONS:
        - Respond ONLY with a valid JSON object following this schema.
        - Do NOT include explanations, reasoning, or text outside the JSON.
        """;
        return """
        {
          "max_tokens": 1024,
          "temperature": 0,
          "messages": [
            {
              "role": "system",
              "content": [
                {
                  "type": "text",
                  "text": "%s"
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
        """.formatted(
            systemPrompt.replace("\"", "\\\"").replace("\n", "\\n"),
            userQuery.replace("\"", "\\\"").replace("\n", "\\n")
        );
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
