package com.example.ai_nl_search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OrderFilter {

    @JsonProperty("location_name")
    private String locationName;
    
    @JsonProperty("date_from")
    private String dateFrom;
    
    @JsonProperty("date_to")
    private String dateTo;
    
    @JsonProperty("exclude_status")
    private List<String> excludeStatus;
    
    @JsonProperty("collected_by")
    private List<String> collectedBy;

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public List<String> getExcludeStatus() {
        return excludeStatus;
    }

    public void setExcludeStatus(List<String> excludeStatus) {
        this.excludeStatus = excludeStatus;
    }

    public List<String> getCollectedBy() {
        return collectedBy;
    }

    public void setCollectedBy(List<String> collectedBy) {
        this.collectedBy = collectedBy;
    }
}
