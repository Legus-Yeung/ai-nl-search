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
    
    @JsonProperty("date_field")
    private String dateField;
    
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
    
    @JsonProperty("exclude_location_name")
    private String excludeLocationName;
    
    @JsonProperty("exclude_location_type")
    private List<String> excludeLocationType;
    
    @JsonProperty("exclude_city")
    private String excludeCity;
    
    @JsonProperty("exclude_company_name")
    private String excludeCompanyName;
    
    @JsonProperty("exclude_carrier_name")
    private String excludeCarrierName;
    
    @JsonProperty("exclude_service")
    private List<String> excludeService;
    
    @JsonProperty("exclude_collected_by")
    private List<String> excludeCollectedBy;
    
    @JsonProperty("flags")
    private List<String> flags;
    
    @JsonProperty("exclude_flags")
    private List<String> excludeFlags;

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
    
    public String getExcludeLocationName() {
        return excludeLocationName;
    }
    
    public void setExcludeLocationName(String excludeLocationName) {
        this.excludeLocationName = excludeLocationName;
    }
    
    public List<String> getExcludeLocationType() {
        return excludeLocationType;
    }
    
    public void setExcludeLocationType(List<String> excludeLocationType) {
        this.excludeLocationType = excludeLocationType;
    }
    
    public String getExcludeCity() {
        return excludeCity;
    }
    
    public void setExcludeCity(String excludeCity) {
        this.excludeCity = excludeCity;
    }
    
    public String getExcludeCompanyName() {
        return excludeCompanyName;
    }
    
    public void setExcludeCompanyName(String excludeCompanyName) {
        this.excludeCompanyName = excludeCompanyName;
    }
    
    public String getExcludeCarrierName() {
        return excludeCarrierName;
    }
    
    public void setExcludeCarrierName(String excludeCarrierName) {
        this.excludeCarrierName = excludeCarrierName;
    }
    
    public List<String> getExcludeService() {
        return excludeService;
    }
    
    public void setExcludeService(List<String> excludeService) {
        this.excludeService = excludeService;
    }
    
    public List<String> getExcludeCollectedBy() {
        return excludeCollectedBy;
    }
    
    public void setExcludeCollectedBy(List<String> excludeCollectedBy) {
        this.excludeCollectedBy = excludeCollectedBy;
    }
    
    public List<String> getFlags() {
        return flags;
    }
    
    public void setFlags(List<String> flags) {
        this.flags = flags;
    }
    
    public List<String> getExcludeFlags() {
        return excludeFlags;
    }
    
    public void setExcludeFlags(List<String> excludeFlags) {
        this.excludeFlags = excludeFlags;
    }
    
    public String getDateField() {
        return dateField;
    }
    
    public void setDateField(String dateField) {
        this.dateField = dateField;
    }
}
