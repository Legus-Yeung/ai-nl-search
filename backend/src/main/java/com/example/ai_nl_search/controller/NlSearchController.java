package com.example.ai_nl_search.controller;

import com.example.ai_nl_search.dto.NlSearchResponse;
import com.example.ai_nl_search.dto.OrderFilter;
import com.example.ai_nl_search.service.BedrockNlService;
import com.example.ai_nl_search.service.OrderSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NlSearchController {

    private final BedrockNlService bedrockNlService;
    private final OrderSearchService orderSearchService;

    @Autowired
    public NlSearchController(BedrockNlService bedrockNlService, OrderSearchService orderSearchService) {
        this.bedrockNlService = bedrockNlService;
        this.orderSearchService = orderSearchService;
    }

    @PostMapping("/nl-search")
    public NlSearchResponse nlSearch(@RequestBody NlSearchRequest request) {
        // Step 1: Convert natural language to OrderFilter JSON
        OrderFilter filter = bedrockNlService.interpret(request.getQuery());
        
        // Step 2: Convert OrderFilter to SQL and execute query
        List<Map<String, Object>> results = orderSearchService.search(filter);
        
        // Step 3: Return results with filters
        NlSearchResponse response = new NlSearchResponse();
        response.setFilters(filter);
        response.setResults(results);
        
        return response;
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
