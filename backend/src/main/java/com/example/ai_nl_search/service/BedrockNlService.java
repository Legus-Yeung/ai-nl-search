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

import java.util.List;
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
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("Unable to load credentials") || 
                                    errorMsg.contains("No credentials") ||
                                    errorMsg.contains("credentials"))) {
                throw new RuntimeException("AWS credentials not configured. Please set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables.", e);
            }
            throw new RuntimeException("Failed to interpret natural language query: " + e.getMessage(), e);
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
        - Valid FLAGS: FRAGILE, VIP, EXPIRED (flags are comma-separated in the database, e.g., "FRAGILE", "VIP", "EXPIRED,VIP")
        - Sample LOCATIONS: "alfred24 Office Locker", "Location A", "Location B", "7Eleven Kwai Chung - PUDO", "ABI Graphique Demo Locker Normal", "ABI Graphique Demo Locker Temp Control"
        - Sample CITIES: "Hong Kong", "Paris"
        - Sample COMPANIES: "Demo Company", "7Eleven Kwai Chung", "ABI Graphique Demo"
        - Sample CARRIERS: "DHL", "SF Express", "UPS"
        
        JSON SCHEMA (all fields are optional; use null if not specified):
        {
          "location_name": string or null (partial match for inclusion),
          "location_type": string or null (exact match for inclusion: PUDO, LOCKER, WAREHOUSE, STORE),
          "date_from": string in YYYY-MM-DD format or null (filters the date field specified in date_field),
          "date_to": string in YYYY-MM-DD format or null (filters the date field specified in date_field),
          "date_field": string or null (valid: CREATED, STORED, COLLECTED - defaults to CREATED if not specified),
          "exclude_status": array of strings or null (valid: CREATED, COURIER_STORED, CUSTOMER_STORED, DELIVERED, OPERATOR_COLLECTED, EXPIRED),
          "collected_by": array of strings or null (valid: COURIER, CUSTOMER, OPERATOR - for inclusion),
          "service": array of strings or null (valid: DELIVERY, RETURNS - for inclusion),
          "city": string or null (exact match for inclusion),
          "company_name": string or null (partial match for inclusion),
          "carrier_name": string or null (partial match for inclusion),
          "exclude_location_name": string or null (partial match for exclusion),
          "exclude_location_type": array of strings or null (exact match for exclusion: PUDO, LOCKER, WAREHOUSE, STORE),
          "exclude_city": string or null (exact match for exclusion),
          "exclude_company_name": string or null (partial match for exclusion),
          "exclude_carrier_name": string or null (partial match for exclusion),
          "exclude_service": array of strings or null (valid: DELIVERY, RETURNS - for exclusion),
          "exclude_collected_by": array of strings or null (valid: COURIER, CUSTOMER, OPERATOR - for exclusion),
          "flags": array of strings or null (valid: FRAGILE, VIP, EXPIRED - for inclusion),
          "exclude_flags": array of strings or null (valid: FRAGILE, VIP, EXPIRED - for exclusion)
        }
        
        INTERPRETATION RULES:
        - "exclude expired" / "not expired" / "excluding expired" → exclude_status: ["EXPIRED"]
        - "show expired orders" / "all expired" → exclude_status: ["CREATED","COURIER_STORED","CUSTOMER_STORED","DELIVERED","OPERATOR_COLLECTED"]
        - "delivered orders" → exclude_status: ["CREATED","COURIER_STORED","CUSTOMER_STORED","OPERATOR_COLLECTED","EXPIRED"]
        - Mention of location name → location_name (partial match for inclusion)
        - "excluding [location name]" / "not [location name]" / "exclude [location name]" → exclude_location_name
        - Mention of location type ("locker", "pudo", "warehouse", "store") → location_type (exact match for inclusion)
        - "excluding [location type]" / "not [location type]" / "exclude [location type]" → exclude_location_type
        - Mention of company name → company_name (partial match for inclusion)
        - "excluding [company name]" / "not [company name]" / "exclude [company name]" / "excluding demo company" → exclude_company_name
        - Mention of carrier name → carrier_name (partial match for inclusion)
        - "excluding [carrier name]" / "not [carrier name]" / "exclude [carrier name]" → exclude_carrier_name
        - Mention of city → city (exact match for inclusion)
        - "excluding [city]" / "not [city]" / "exclude [city]" → exclude_city
        - Mention of service type ("delivery" / "returns") → service (for inclusion)
        - "excluding [service]" / "not [service]" / "exclude [service]" → exclude_service
        - Mention of collected by type → collected_by (for inclusion)
        - "excluding [collected_by]" / "not [collected_by]" / "exclude [collected_by]" → exclude_collected_by
        - Mention of date range → date_from and/or date_to (convert relative dates to YYYY-MM-DD format)
        - Relative dates: "today" → current date in YYYY-MM-DD, "last 7 days" → date_from = 7 days ago, "last month" → date_from = 1 month ago
        - Date field keywords: "created" / "creation" → date_field: "CREATED", "stored" → date_field: "STORED", "collected" → date_field: "COLLECTED"
        - If date field not specified, default to CREATED (time_created)
        - Date examples: "from Jan 1 to today" → date_from: "2026-01-01", date_to: current date, "last 7 days" → date_from: 7 days ago, date_to: today
        - Mention of flags ("fragile", "vip", "expired") → flags (for inclusion, use uppercase: FRAGILE, VIP, EXPIRED)
        - "excluding [flag]" / "not [flag]" / "exclude [flag]" → exclude_flags (use uppercase: FRAGILE, VIP, EXPIRED)
        - Only include fields that can be explicitly inferred; use null for all unspecified fields
        - Inclusion and exclusion can be combined (e.g., "delivery orders excluding demo company" → service: ["DELIVERY"], exclude_company_name: "Demo Company")
        
        EXAMPLES:
        - "Show me all orders excluding expired" →
          {"exclude_status":["EXPIRED"],"location_name":null,"location_type":null,"date_from":null,"date_to":null,"date_field":null,"collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":null}
        
        - "Show me all orders excluding demo company" →
          {"exclude_status":null,"location_name":null,"location_type":null,"date_from":null,"date_to":null,"date_field":null,"collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":"Demo Company","exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":null}
        
        - "Show me all orders that are fragile" →
          {"exclude_status":null,"location_name":null,"location_type":null,"date_from":null,"date_to":null,"date_field":null,"collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":["FRAGILE"],"exclude_flags":null}
        
        - "Show expired orders at Location A" →
          {"exclude_status":["CREATED","COURIER_STORED","CUSTOMER_STORED","DELIVERED","OPERATOR_COLLECTED"],"location_name":"Location A","location_type":null,"date_from":null,"date_to":null,"date_field":null,"collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":null}
        
        - "Delivered orders at ABI Graphique Demo Locker Normal" →
          {"exclude_status":["CREATED","COURIER_STORED","CUSTOMER_STORED","OPERATOR_COLLECTED","EXPIRED"],"location_name":"ABI Graphique Demo Locker Normal","location_type":"LOCKER","date_from":null,"date_to":null,"date_field":null,"collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":null}
        
        - "All DHL deliveries at lockers in Hong Kong from January 2026" →
          {"exclude_status":null,"location_name":null,"location_type":"LOCKER","date_from":"2026-01-01","date_to":"2026-01-31","date_field":"CREATED","collected_by":null,"service":["DELIVERY"],"city":"Hong Kong","company_name":null,"carrier_name":"DHL","exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":null}
        
        - "Orders stored in the last 7 days" →
          {"exclude_status":null,"location_name":null,"location_type":null,"date_from":null,"date_to":null,"date_field":"STORED","collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":null}
        
        - "Orders collected today" →
          {"exclude_status":null,"location_name":null,"location_type":null,"date_from":null,"date_to":null,"date_field":"COLLECTED","collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":null}
        
        - "Orders from January 2026 excluding expired for 7Eleven Kwai Chung" →
          {"exclude_status":["EXPIRED"],"location_name":null,"location_type":null,"date_from":"2026-01-01","date_to":"2026-01-31","date_field":"CREATED","collected_by":null,"service":null,"city":null,"company_name":"7Eleven Kwai Chung","carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":null}
        
        - "Delivery orders excluding demo company" →
          {"exclude_status":null,"location_name":null,"location_type":null,"date_from":null,"date_to":null,"date_field":null,"collected_by":null,"service":["DELIVERY"],"city":null,"company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":"Demo Company","exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":null}
        
        - "Orders excluding lockers in Hong Kong" →
          {"exclude_status":null,"location_name":null,"location_type":null,"date_from":null,"date_to":null,"date_field":null,"collected_by":null,"service":null,"city":"Hong Kong","company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":["LOCKER"],"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":null}
        
        - "All orders excluding DHL carrier" →
          {"exclude_status":null,"location_name":null,"location_type":null,"date_from":null,"date_to":null,"date_field":null,"collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":"DHL","exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":null}
        
        - "Returns service orders excluding customer collected" →
          {"exclude_status":null,"location_name":null,"location_type":null,"date_from":null,"date_to":null,"date_field":null,"collected_by":null,"service":["RETURNS"],"city":null,"company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":["CUSTOMER"],"flags":null,"exclude_flags":null}
        
        - "Orders excluding fragile items" →
          {"exclude_status":null,"location_name":null,"location_type":null,"date_from":null,"date_to":null,"date_field":null,"collected_by":null,"service":null,"city":null,"company_name":null,"carrier_name":null,"exclude_location_name":null,"exclude_location_type":null,"exclude_city":null,"exclude_company_name":null,"exclude_carrier_name":null,"exclude_service":null,"exclude_collected_by":null,"flags":null,"exclude_flags":["FRAGILE"]}
        
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
    
    private String cleanFollowUpResponse(String textResponse) {
        if (textResponse == null || textResponse.trim().isEmpty()) {
            return null;
        }
        
        textResponse = textResponse.replaceAll("(?i)(?s)<reasoning>.*?</reasoning>", "");
        textResponse = textResponse.replaceAll("(?i)(?s)<thinking>.*?</thinking>", "");
        textResponse = textResponse.replaceAll("(?i)(?s)<analysis>.*?</analysis>", "");
        textResponse = textResponse.replaceAll("(?s)<[^>]+>", "");
        textResponse = textResponse.replaceAll("(?s)```[\\s\\S]*?```", "");
        
        String[] lines = textResponse.split("\n");
        StringBuilder cleaned = new StringBuilder();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String lowerLine = line.toLowerCase();
            if (lowerLine.startsWith("we have") ||
                lowerLine.startsWith("applied filter") ||
                lowerLine.startsWith("result count") ||
                lowerLine.startsWith("assumptions made") ||
                lowerLine.startsWith("so we") ||
                lowerLine.startsWith("according to") ||
                lowerLine.startsWith("the format") ||
                lowerLine.startsWith("use the") ||
                lowerLine.startsWith("that matches") ||
                lowerLine.startsWith("so we output") ||
                lowerLine.startsWith("we need to") ||
                lowerLine.startsWith("we should") ||
                lowerLine.contains("behavior") && lowerLine.contains("choose")) {
                continue;
            }
            if (cleaned.length() > 0) {
                cleaned.append(" ");
            }
            cleaned.append(line);
        }     
        textResponse = cleaned.toString().trim();
        
        if (textResponse.startsWith("\"") && textResponse.endsWith("\"")) {
            textResponse = textResponse.substring(1, textResponse.length() - 1);
        }
        if (textResponse.startsWith("'") && textResponse.endsWith("'")) {
            textResponse = textResponse.substring(1, textResponse.length() - 1);
        }
        
        textResponse = textResponse.trim();
        
        return textResponse.isEmpty() ? null : textResponse;
    }
    
    public String generateFollowUp(String originalQuery, OrderFilter filter, int resultCount, 
                                   List<String> assumptions) {
        try {
            String requestBody = buildFollowUpRequest(originalQuery, filter, resultCount, assumptions);
            
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(MODEL_ID)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(requestBody))
                    .build();
            
            InvokeModelResponse response = client.invokeModel(request);
            String responseJson = response.body().asUtf8String();
            String textResponse = extractTextResponse(responseJson);
            
            textResponse = cleanFollowUpResponse(textResponse);
            
            if (textResponse == null || textResponse.isEmpty() ||
                textResponse.toLowerCase().contains("NO_ASSUMPTION_MADE") || 
                textResponse.toLowerCase().contains("no assumptions")) {
                return null;
            }
            
            return textResponse;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private String buildFollowUpRequest(String originalQuery, OrderFilter filter, int resultCount,
                                       List<String> assumptions) {
        String systemPrompt = """
            You are a helpful assistant that generates follow-up messages for order search queries.
            
            You must generate ONE of these 4 specific behaviors (choose the most appropriate):
            
            BEHAVIOR 1 - Suggest valid searches (when results are empty or query is ambiguous):
            Format: "Did you mean [suggestion]?" or "No orders found. Try checking [location/company/carrier] names."
            Use when: Results are empty and there might be similar valid values.
            Example: "No orders found for 'Location X'. Did you mean Location A or Location B?"
            Note: Use the valid values below to suggest realistic alternatives when appropriate.
            
            BEHAVIOR 2 - Notify user of assumption made:
            Format: Clearly state what assumption was made from query to filter.
            Use when: An assumption was made (e.g., date field defaulted to CREATED).
            Example: "Filtering by order creation time. To filter by stored or collected time, specify 'stored' or 'collected'."
            Important: When a date assumption is mentioned in the assumptions list, you MUST generate a follow-up message explaining this to the user. 
            Be specific: "Filtering by order creation date. To filter by stored or collected date, add 'stored' or 'collected' to your query."
            
            BEHAVIOR 3 - No assumption made:
            Format: Return exactly "NO_ASSUMPTION_MADE"
            Use when: Query was clear, no assumptions were made, results are not empty, and query is clearly order-related.
            
            BEHAVIOR 4 - Not order-related query:
            Format: Politely tell user to type about order searches only.
            Use when: Query doesn't seem related to order searching at all.
            Example: "This search is for orders only. Please ask about orders, locations, statuses, dates, etc."
            
            RULES:
            - Keep messages to 1-2 lines maximum
            - Be helpful and specific
            - For BEHAVIOR 3, return exactly "NO_ASSUMPTION_MADE" (this will be hidden from user)
            - For BEHAVIOR 1, suggest realistic alternatives using valid values below when relevant
            - For BEHAVIOR 2, clearly explain what assumption was made
            - CRITICAL: If assumptions list contains any date-related assumption, you MUST use BEHAVIOR 2 and generate a message about it
            - Do NOT return "NO_ASSUMPTION_MADE" if there are any assumptions in the assumptions list
            
            VALID DATA EXAMPLES (for reference when suggesting alternatives):
            - Valid STATUSES: CREATED, COURIER_STORED, CUSTOMER_STORED, DELIVERED, OPERATOR_COLLECTED, EXPIRED
            - Valid SERVICES: DELIVERY, RETURNS
            - Valid LOCATION_TYPES: LOCKER, STORE, PUDO, WAREHOUSE
            - Valid COLLECTED_BY: COURIER, CUSTOMER, OPERATOR
            - Valid FLAGS: FRAGILE, VIP, EXPIRED (can be comma-separated like "EXPIRED,VIP")
            - Sample CITIES: Hong Kong, Paris
            - Sample COMPANIES: Demo Company, 7Eleven Kwai Chung, ABI Graphique Demo
            - Sample CARRIERS: DHL, SF Express, UPS
            - Sample LOCATIONS: alfred24 Office Locker, 7Eleven Kwai Chung - PUDO, Location A, ABI Graphique Demo Locker Normal
            
            CRITICAL: Respond with ONLY the follow-up message text itself (or "NO_ASSUMPTION_MADE" for case 3).
            DO NOT include:
            - Any reasoning, thinking, or analysis
            - Any XML tags like <reasoning> or <thinking>
            - Any explanations or step-by-step logic
            - Any meta-commentary about your decision process
            - Just output the final message that should be shown to the user
            
            Example of CORRECT output:
            "No orders found for 'Location B'. Did you mean Location A or alfred24 Office Locker?"
            
            Example of INCORRECT output (DO NOT DO THIS):
            <reasoning>We have a query... So we output that.</reasoning>No orders found for 'Location B'. Did you mean Location A or alfred24 Office Locker?
            
            """;
        
        String filterJson = "";
        try {
            filterJson = objectMapper.writeValueAsString(filter);
        } catch (Exception e) {
            filterJson = "{}";
        }
        
        String assumptionsText = assumptions.isEmpty() ? "None" : String.join("; ", assumptions);
        
        String fullPrompt = systemPrompt +
            "\n\nOriginal query: " + originalQuery +
            "\nApplied filter: " + filterJson +
            "\nResult count: " + resultCount +
            "\nAssumptions made: " + assumptionsText +
            "\n\nGenerate the appropriate follow-up message:";
        
        return """
        {
          "max_tokens": 1024,
          "temperature": 0.3,
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
                  "text": "Generate the follow-up message."
                }
              ]
            }
          ]
        }
        """.formatted(fullPrompt.replace("\"", "\\\"").replace("\n", "\\n"));
    }
}
