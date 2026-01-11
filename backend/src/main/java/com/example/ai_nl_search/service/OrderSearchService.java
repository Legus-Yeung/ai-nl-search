package com.example.ai_nl_search.service;

import com.example.ai_nl_search.dto.OrderFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderSearchService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OrderSearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void addWordBasedMatch(StringBuilder sql, List<Object> params, String column, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return;
        }
        
        List<String> words = Arrays.stream(searchTerm.trim().split("\\s+"))
            .filter(word -> !word.isEmpty())
            .collect(Collectors.toList());
        
        if (words.isEmpty()) {
            return;
        }
        
        sql.append(" AND (");
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append("LOWER(").append(column).append(") LIKE LOWER(?)");
            params.add("%" + words.get(i) + "%");
        }
        sql.append(")");
    }

    public List<Map<String, Object>> search(OrderFilter filter) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                o.id,
                o.tracking_no,
                o.order_no,
                o.service,
                o.status,
                o.collected_by_type,
                o.recipient_phone,
                o.compartment_no,
                o.time_created,
                o.time_stored,
                o.time_collected,
                o.expires_at,
                o.flags,
                l.name AS location_name,
                l.location_type,
                l.city,
                c.name AS company_name,
                car.name AS carrier_name
            FROM orders o
            INNER JOIN locations l ON o.location_id = l.id
            INNER JOIN companies c ON o.company_id = c.id
            LEFT JOIN carriers car ON o.carrier_id = car.id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (filter.getLocationName() != null && !filter.getLocationName().trim().isEmpty()) {
            addWordBasedMatch(sql, params, "l.name", filter.getLocationName());
        }
        
        if (filter.getLocationType() != null && !filter.getLocationType().trim().isEmpty()) {
            sql.append(" AND l.location_type = ?");
            params.add(filter.getLocationType().trim());
        }

        String dateField = filter.getDateField();
        if (dateField == null || dateField.trim().isEmpty()) {
            dateField = "CREATED";
        }
        
        String dateColumn;
        switch (dateField.toUpperCase()) {
            case "STORED":
                dateColumn = "o.time_stored";
                break;
            case "COLLECTED":
                dateColumn = "o.time_collected";
                break;
            case "CREATED":
            default:
                dateColumn = "o.time_created";
                break;
        }
        
        if (filter.getDateFrom() != null && !filter.getDateFrom().trim().isEmpty()) {
            sql.append(" AND DATE(").append(dateColumn).append(") >= ?");
            params.add(filter.getDateFrom().trim());
        }

        if (filter.getDateTo() != null && !filter.getDateTo().trim().isEmpty()) {
            sql.append(" AND DATE(").append(dateColumn).append(") <= ?");
            params.add(filter.getDateTo().trim());
        }

        if (filter.getExcludeStatus() != null && !filter.getExcludeStatus().isEmpty()) {
            sql.append(" AND o.status NOT IN (");
            for (int i = 0; i < filter.getExcludeStatus().size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
                params.add(filter.getExcludeStatus().get(i));
            }
            sql.append(")");
        }
        
        if (filter.getService() != null && !filter.getService().isEmpty()) {
            sql.append(" AND o.service IN (");
            for (int i = 0; i < filter.getService().size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
                params.add(filter.getService().get(i));
            }
            sql.append(")");
        }
        
        if (filter.getCity() != null && !filter.getCity().trim().isEmpty()) {
            addWordBasedMatch(sql, params, "l.city", filter.getCity());
        }
        
        if (filter.getCompanyName() != null && !filter.getCompanyName().trim().isEmpty()) {
            addWordBasedMatch(sql, params, "c.name", filter.getCompanyName());
        }
        
        if (filter.getCarrierName() != null && !filter.getCarrierName().trim().isEmpty()) {
            addWordBasedMatch(sql, params, "car.name", filter.getCarrierName());
        }

        if (filter.getCollectedBy() != null && !filter.getCollectedBy().isEmpty()) {
            sql.append(" AND o.collected_by_type IN (");
            for (int i = 0; i < filter.getCollectedBy().size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
                params.add(filter.getCollectedBy().get(i));
            }
            sql.append(")");
        }

        if (filter.getExcludeLocationName() != null && !filter.getExcludeLocationName().trim().isEmpty()) {
            List<String> words = Arrays.stream(filter.getExcludeLocationName().trim().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
            
            if (!words.isEmpty()) {
                sql.append(" AND NOT (");
                for (int i = 0; i < words.size(); i++) {
                    if (i > 0) {
                        sql.append(" OR ");
                    }
                    sql.append("LOWER(l.name) LIKE LOWER(?)");
                    params.add("%" + words.get(i) + "%");
                }
                sql.append(")");
            }
        }
        
        if (filter.getExcludeLocationType() != null && !filter.getExcludeLocationType().isEmpty()) {
            sql.append(" AND l.location_type NOT IN (");
            for (int i = 0; i < filter.getExcludeLocationType().size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
                params.add(filter.getExcludeLocationType().get(i));
            }
            sql.append(")");
        }
        
        if (filter.getExcludeCity() != null && !filter.getExcludeCity().trim().isEmpty()) {
            List<String> words = Arrays.stream(filter.getExcludeCity().trim().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
            
            if (!words.isEmpty()) {
                sql.append(" AND NOT (");
                for (int i = 0; i < words.size(); i++) {
                    if (i > 0) {
                        sql.append(" OR ");
                    }
                    sql.append("LOWER(l.city) LIKE LOWER(?)");
                    params.add("%" + words.get(i) + "%");
                }
                sql.append(")");
            }
        }
        
        if (filter.getExcludeCompanyName() != null && !filter.getExcludeCompanyName().trim().isEmpty()) {
            List<String> words = Arrays.stream(filter.getExcludeCompanyName().trim().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
            
            if (!words.isEmpty()) {
                sql.append(" AND NOT (");
                for (int i = 0; i < words.size(); i++) {
                    if (i > 0) {
                        sql.append(" OR ");
                    }
                    sql.append("LOWER(c.name) LIKE LOWER(?)");
                    params.add("%" + words.get(i) + "%");
                }
                sql.append(")");
            }
        }
        
        if (filter.getExcludeCarrierName() != null && !filter.getExcludeCarrierName().trim().isEmpty()) {
            List<String> words = Arrays.stream(filter.getExcludeCarrierName().trim().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
            
            if (!words.isEmpty()) {
                sql.append(" AND (car.name IS NULL OR NOT (");
                for (int i = 0; i < words.size(); i++) {
                    if (i > 0) {
                        sql.append(" OR ");
                    }
                    sql.append("LOWER(car.name) LIKE LOWER(?)");
                    params.add("%" + words.get(i) + "%");
                }
                sql.append("))");
            }
        }
        
        if (filter.getExcludeService() != null && !filter.getExcludeService().isEmpty()) {
            sql.append(" AND o.service NOT IN (");
            for (int i = 0; i < filter.getExcludeService().size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
                params.add(filter.getExcludeService().get(i));
            }
            sql.append(")");
        }
        
        if (filter.getExcludeCollectedBy() != null && !filter.getExcludeCollectedBy().isEmpty()) {
            sql.append(" AND (o.collected_by_type IS NULL OR o.collected_by_type NOT IN (");
            for (int i = 0; i < filter.getExcludeCollectedBy().size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
                params.add(filter.getExcludeCollectedBy().get(i));
            }
            sql.append("))");
        }
        
        if (filter.getFlags() != null && !filter.getFlags().isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < filter.getFlags().size(); i++) {
                if (i > 0) sql.append(" OR ");
                sql.append("FIND_IN_SET(?, o.flags) > 0");
                params.add(filter.getFlags().get(i));
            }
            sql.append(")");
        }
        
        if (filter.getExcludeFlags() != null && !filter.getExcludeFlags().isEmpty()) {
            sql.append(" AND (o.flags IS NULL OR ");
            for (int i = 0; i < filter.getExcludeFlags().size(); i++) {
                if (i > 0) sql.append(" AND ");
                sql.append("FIND_IN_SET(?, o.flags) = 0");
                params.add(filter.getExcludeFlags().get(i));
            }
            sql.append(")");
        }

        sql.append(" ORDER BY o.time_created DESC");

        return jdbcTemplate.query(sql.toString(), params.toArray(new Object[0]), (rs, rowNum) -> {
            Map<String, Object> order = new HashMap<>();
            order.put("id", rs.getLong("id"));
            order.put("trackingNo", rs.getString("tracking_no"));
            order.put("orderNo", rs.getString("order_no"));
            order.put("service", rs.getString("service"));
            order.put("status", rs.getString("status"));
            order.put("collectedByType", rs.getString("collected_by_type"));
            order.put("recipientPhone", rs.getString("recipient_phone"));
            order.put("compartmentNo", rs.getObject("compartment_no") != null ? rs.getInt("compartment_no") : null);
            order.put("timeCreated", rs.getTimestamp("time_created"));
            order.put("timeStored", rs.getTimestamp("time_stored"));
            order.put("timeCollected", rs.getTimestamp("time_collected"));
            order.put("expiresAt", rs.getTimestamp("expires_at"));
            order.put("flags", rs.getString("flags"));
            order.put("locationName", rs.getString("location_name"));
            order.put("locationType", rs.getString("location_type"));
            order.put("city", rs.getString("city"));
            order.put("companyName", rs.getString("company_name"));
            order.put("carrierName", rs.getString("carrier_name"));
            return order;
        });
    }
}
