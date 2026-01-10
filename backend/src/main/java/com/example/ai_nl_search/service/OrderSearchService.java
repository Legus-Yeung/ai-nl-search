package com.example.ai_nl_search.service;

import com.example.ai_nl_search.dto.OrderFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderSearchService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OrderSearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
            sql.append(" AND l.name LIKE ?");
            params.add("%" + filter.getLocationName().trim() + "%");
        }

        if (filter.getDateFrom() != null && !filter.getDateFrom().trim().isEmpty()) {
            sql.append(" AND DATE(o.time_created) >= ?");
            params.add(filter.getDateFrom().trim());
        }

        if (filter.getDateTo() != null && !filter.getDateTo().trim().isEmpty()) {
            sql.append(" AND DATE(o.time_created) <= ?");
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

        if (filter.getCollectedBy() != null && !filter.getCollectedBy().isEmpty()) {
            sql.append(" AND o.collected_by_type IN (");
            for (int i = 0; i < filter.getCollectedBy().size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("?");
                params.add(filter.getCollectedBy().get(i));
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
