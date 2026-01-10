package com.example.ai_nl_search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OrderFilter {

    @JsonProperty("location_name")
    private String locationName;
    
    @JsonProperty("location_type")
    private String locationType;
    
    @JsonProperty("date_from")
    private String dateFrom;
    
    @JsonProperty("date_to")
    private String dateTo;
    
    @JsonProperty("exclude_status")
    private List<String> excludeStatus;
    
    @JsonProperty("collected_by")
    private List<String> collectedBy;
    
    @JsonProperty("service")
    private List<String> service;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("company_name")
    private String companyName;
    
    @JsonProperty("carrier_name")
    private String carrierName;

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
    
    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
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
    
    public List<String> getService() {
        return service;
    }

    public void setService(List<String> service) {
        this.service = service;
    }
    
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
    
    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public List<String> getCollectedBy() {
        return collectedBy;
    }

    public void setCollectedBy(List<String> collectedBy) {
        this.collectedBy = collectedBy;
    }
}
