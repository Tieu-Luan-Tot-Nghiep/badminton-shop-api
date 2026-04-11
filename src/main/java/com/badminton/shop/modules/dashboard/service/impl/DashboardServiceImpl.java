package com.badminton.shop.modules.dashboard.service.impl;

import com.badminton.shop.modules.dashboard.dto.response.*;
import com.badminton.shop.modules.dashboard.service.DashboardService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<RevenueReportResponse> getRevenueByPeriod(LocalDateTime startDate, LocalDateTime endDate, String groupBy) {
        String dateFormat;
        switch (groupBy.toUpperCase()) {
            case "MONTH":
                dateFormat = "YYYY-MM";
                break;
            case "YEAR":
                dateFormat = "YYYY";
                break;
            case "DAY":
            default:
                dateFormat = "YYYY-MM-DD";
                break;
        }

        String sql = String.format("""
            SELECT 
                TO_CHAR(o.created_at, '%s') as period,
                SUM(o.total_amount) as totalRevenue,
                COUNT(o.id) as totalOrders
            FROM orders o
            WHERE o.status NOT IN ('CANCELLED', 'RETURNED', 'REFUNDED') 
              AND o.created_at >= :startDate 
              AND o.created_at <= :endDate
            GROUP BY TO_CHAR(o.created_at, '%s')
            ORDER BY TO_CHAR(o.created_at, '%s') ASC
        """, dateFormat, dateFormat, dateFormat);

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate);

        List<Object[]> results = query.getResultList();
        List<RevenueReportResponse> responses = new ArrayList<>();
        
        for (Object[] row : results) {
            String period = (String) row[0];
            BigDecimal totalRevenue = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            Long totalOrders = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            responses.add(new RevenueReportResponse(period, totalRevenue, totalOrders));
        }
        
        return responses;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BrandRevenueResponse> getRevenueByBrand(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT 
                b.name as brandName,
                SUM(oi.quantity * oi.price_at_purchase) as totalRevenue,
                SUM(oi.quantity) as itemsSold
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            JOIN product_variants pv ON oi.variant_id = pv.id
            JOIN products p ON pv.product_id = p.id
            JOIN brands b ON p.brand_id = b.id
            WHERE o.status NOT IN ('CANCELLED', 'RETURNED', 'REFUNDED')
              AND o.created_at >= :startDate 
              AND o.created_at <= :endDate
            GROUP BY b.name
            ORDER BY totalRevenue DESC
        """;

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate);

        List<Object[]> results = query.getResultList();
        List<BrandRevenueResponse> responses = new ArrayList<>();
        
        for (Object[] row : results) {
            String brandName = (String) row[0];
            BigDecimal totalRevenue = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            Long itemsSold = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            responses.add(new BrandRevenueResponse(brandName, totalRevenue, itemsSold));
        }
        
        return responses;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TopSellingResponse> getTopSellingProducts(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        String sql = """
            SELECT 
                p.id as productId,
                p.name as productName,
                p.slug as slug,
                p.thumbnail_url as thumbnailUrl,
                SUM(oi.quantity) as totalQuantitySold,
                SUM(oi.quantity * oi.price_at_purchase) as totalRevenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            JOIN product_variants pv ON oi.variant_id = pv.id
            JOIN products p ON pv.product_id = p.id
            WHERE o.status NOT IN ('CANCELLED', 'RETURNED', 'REFUNDED')
              AND o.created_at >= :startDate 
              AND o.created_at <= :endDate
            GROUP BY p.id, p.name, p.slug, p.thumbnail_url
            ORDER BY totalQuantitySold DESC
        """;

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setMaxResults(limit);

        List<Object[]> results = query.getResultList();
        List<TopSellingResponse> responses = new ArrayList<>();
        
        for (Object[] row : results) {
            Long productId = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            String productName = (String) row[1];
            String slug = (String) row[2];
            String thumbnailUrl = (String) row[3];
            Long totalQuantitySold = row[4] != null ? ((Number) row[4]).longValue() : 0L;
            BigDecimal totalRevenue = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;
            
            responses.add(new TopSellingResponse(productId, productName, slug, thumbnailUrl, totalQuantitySold, totalRevenue));
        }
        
        return responses;
    }

    @Override
    public InventoryValueResponse getInventoryValue() {
        String sql = """
            SELECT 
                SUM(i.available_quantity) as totalStockQuantity,
                SUM(i.available_quantity * pv.price) as estimatedValue
            FROM inventories i
            JOIN product_variants pv ON i.variant_id = pv.id
            WHERE i.available_quantity > 0 AND pv.is_deleted = false
        """;

        Query query = entityManager.createNativeQuery(sql);
        Object[] result = (Object[]) query.getSingleResult();
        
        Long totalStockQuantity = result[0] != null ? ((Number) result[0]).longValue() : 0L;
        BigDecimal estimatedValue = result[1] != null ? new BigDecimal(result[1].toString()) : BigDecimal.ZERO;
        
        return new InventoryValueResponse(totalStockQuantity, estimatedValue);
    }
    @Override
    public java.util.Map<String, Object> getKpis() {
        return java.util.Map.of(
            "totalRevenue", BigDecimal.ZERO,
            "totalOrders", 0,
            "totalCustomers", 0,
            "totalProducts", 0
        );
    }

    @Override
    public List<Object> getRecentOrders() {
        return new ArrayList<>();
    }

    @Override
    public List<Object> getAlerts() {
        return new ArrayList<>();
    }
}
