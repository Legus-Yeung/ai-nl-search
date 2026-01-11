package com.example.ai_nl_search.controller;

import com.example.ai_nl_search.dto.NlSearchResponse;
import com.example.ai_nl_search.dto.OrderFilter;
import com.example.ai_nl_search.service.BedrockNlService;
import com.example.ai_nl_search.service.OrderSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class NlSearchController {

    private static final Set<String> VALID_STATUSES = new HashSet<>(Arrays.asList(
        "CREATED", "COURIER_STORED", "CUSTOMER_STORED", "DELIVERED", "OPERATOR_COLLECTED", "EXPIRED"
    ));
    private static final Set<String> VALID_SERVICES = new HashSet<>(Arrays.asList("DELIVERY", "RETURNS"));
    private static final Set<String> VALID_COLLECTED_BY = new HashSet<>(Arrays.asList("COURIER", "CUSTOMER", "OPERATOR"));
    private static final Set<String> VALID_LOCATION_TYPES = new HashSet<>(Arrays.asList("PUDO", "LOCKER", "WAREHOUSE", "STORE"));
    private static final Set<String> VALID_FLAGS = new HashSet<>(Arrays.asList("FRAGILE", "VIP", "EXPIRED"));
    private static final Set<String> VALID_DATE_FIELDS = new HashSet<>(Arrays.asList("CREATED", "STORED", "COLLECTED"));

    private final BedrockNlService bedrockNlService;
    private final OrderSearchService orderSearchService;

    @Autowired
    public NlSearchController(BedrockNlService bedrockNlService, OrderSearchService orderSearchService) {
        this.bedrockNlService = bedrockNlService;
        this.orderSearchService = orderSearchService;
    }

    @PostMapping("/nl-search")
    public ResponseEntity<?> nlSearch(@RequestBody NlSearchRequest request) {
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Query cannot be empty. Please enter a search query."));
        }

        try {
            String query = request.getQuery().trim();
            
            // Step 1: Convert natural language to OrderFilter JSON
            OrderFilter filter = bedrockNlService.interpret(query);
            
            // Step 2: Validate and sanitize filter, track assumptions
            List<String> warnings = new ArrayList<>();
            List<String> assumptions = validateAndSanitizeFilter(filter, warnings, query);
            
            // Step 3: Convert OrderFilter to SQL and execute query
            List<Map<String, Object>> results = orderSearchService.search(filter);
            
            // Step 4: Generate follow-up message
            String followUp = bedrockNlService.generateFollowUp(
                query, filter, results.size(), assumptions);
            
            // Step 5: Return results with filters, warnings, and follow-up
            NlSearchResponse response = new NlSearchResponse();
            response.setFilters(filter);
            response.setResults(results);
            response.setWarnings(warnings);
            response.setFollowUp(followUp);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("AWS credentials not configured")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", 
                        "AWS credentials are not configured. Please set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables."));
            }
            if (errorMessage != null && errorMessage.contains("Failed to interpret")) {
                System.err.println("Bedrock interpretation error: " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("Cause: " + e.getCause().getMessage());
                    e.getCause().printStackTrace();
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", 
                        "Unable to understand your query. Please try rephrasing. " +
                        "Example: 'Show me all delivered orders from January 2026'"));
            }
            
            System.err.println("Runtime error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An error occurred while processing your search. Please try again."));
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An unexpected error occurred. Please try again later."));
        }
    }
    
    private List<String> validateAndSanitizeFilter(OrderFilter filter, List<String> warnings, String originalQuery) {
        List<String> assumptions = new ArrayList<>();
        
        if (filter.getExcludeStatus() != null) {
            List<String> invalidStatuses = new ArrayList<>();
            for (String status : filter.getExcludeStatus()) {
                if (!VALID_STATUSES.contains(status)) {
                    warnings.add("Unknown status '" + status + "' was ignored. Valid statuses: " + VALID_STATUSES);
                    invalidStatuses.add(status);
                }
            }
            filter.getExcludeStatus().removeAll(invalidStatuses);
        }
        
        if (filter.getService() != null) {
            filter.getService().removeIf(s -> {
                if (!VALID_SERVICES.contains(s)) {
                    warnings.add("Unknown service '" + s + "' was ignored. Valid services: " + VALID_SERVICES);
                    return true;
                }
                return false;
            });
        }
        
        if (filter.getExcludeService() != null) {
            filter.getExcludeService().removeIf(s -> {
                if (!VALID_SERVICES.contains(s)) {
                    warnings.add("Unknown service '" + s + "' was ignored. Valid services: " + VALID_SERVICES);
                    return true;
                }
                return false;
            });
        }
        
        if (filter.getCollectedBy() != null) {
            filter.getCollectedBy().removeIf(c -> {
                if (!VALID_COLLECTED_BY.contains(c)) {
                    warnings.add("Unknown collected_by '" + c + "' was ignored. Valid values: " + VALID_COLLECTED_BY);
                    return true;
                }
                return false;
            });
        }
        
        if (filter.getExcludeCollectedBy() != null) {
            filter.getExcludeCollectedBy().removeIf(c -> {
                if (!VALID_COLLECTED_BY.contains(c)) {
                    warnings.add("Unknown collected_by '" + c + "' was ignored. Valid values: " + VALID_COLLECTED_BY);
                    return true;
                }
                return false;
            });
        }
        
        if (filter.getLocationType() != null && !VALID_LOCATION_TYPES.contains(filter.getLocationType())) {
            warnings.add("Unknown location type '" + filter.getLocationType() + "' was ignored. Valid types: " + VALID_LOCATION_TYPES);
            filter.setLocationType(null);
        }
        
        if (filter.getExcludeLocationType() != null) {
            filter.getExcludeLocationType().removeIf(lt -> {
                if (!VALID_LOCATION_TYPES.contains(lt)) {
                    warnings.add("Unknown location type '" + lt + "' was ignored. Valid types: " + VALID_LOCATION_TYPES);
                    return true;
                }
                return false;
            });
        }
        
        if (filter.getFlags() != null) {
            filter.getFlags().removeIf(f -> {
                if (!VALID_FLAGS.contains(f)) {
                    warnings.add("Unknown flag '" + f + "' was ignored. Valid flags: " + VALID_FLAGS);
                    return true;
                }
                return false;
            });
        }
        
        if (filter.getExcludeFlags() != null) {
            filter.getExcludeFlags().removeIf(f -> {
                if (!VALID_FLAGS.contains(f)) {
                    warnings.add("Unknown flag '" + f + "' was ignored. Valid flags: " + VALID_FLAGS);
                    return true;
                }
                return false;
            });
        }
        
        if (filter.getDateField() != null && !VALID_DATE_FIELDS.contains(filter.getDateField().toUpperCase())) {
            warnings.add("Unknown date field '" + filter.getDateField() + "'. Using CREATED as default.");
            filter.setDateField("CREATED");
        }
        
        if (filter.getDateFrom() != null || filter.getDateTo() != null) {
            String dateField = filter.getDateField();
            if (dateField == null || dateField.isEmpty()) {
                dateField = "CREATED";
                filter.setDateField("CREATED");
            }
            
            String queryLower = originalQuery.toLowerCase();
            boolean userSpecifiedDateField = queryLower.contains("created") || 
                                            queryLower.contains("creation") ||
                                            queryLower.contains("stored") ||
                                            queryLower.contains("collected");
            
            if (dateField.equals("CREATED") && !userSpecifiedDateField) {
                assumptions.add("Date filter defaulted to order creation time. " +
                    "To filter by stored or collected time, specify 'stored' or 'collected' in your query.");
            }
        }
        
        return assumptions;
    }

    public static class NlSearchRequest {
        private String query;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }
}
