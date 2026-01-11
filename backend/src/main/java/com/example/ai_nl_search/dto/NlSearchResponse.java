package com.example.ai_nl_search.dto;

import java.util.List;
import java.util.Map;

public class NlSearchResponse {

    private OrderFilter filters;
    private List<Map<String, Object>> results;
    private List<String> warnings;
    private String followUp;

    public OrderFilter getFilters() {
        return filters;
    }

    public void setFilters(OrderFilter filters) {
        this.filters = filters;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getFollowUp() {
        return followUp;
    }

    public void setFollowUp(String followUp) {
        this.followUp = followUp;
    }
}
