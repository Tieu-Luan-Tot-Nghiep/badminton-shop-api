package com.badminton.shop.modules.dashboard.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.dashboard.dto.response.BrandRevenueResponse;
import com.badminton.shop.modules.dashboard.dto.response.InventoryValueResponse;
import com.badminton.shop.modules.dashboard.dto.response.RevenueReportResponse;
import com.badminton.shop.modules.dashboard.dto.response.TopSellingResponse;
import com.badminton.shop.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<List<RevenueReportResponse>>> getRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String groupBy
    ) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        
        List<RevenueReportResponse> data = dashboardService.getRevenueByPeriod(start, end, groupBy);
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo doanh thu thành công", data));
    }

    @GetMapping("/revenue-by-brand")
    public ResponseEntity<ApiResponse<List<BrandRevenueResponse>>> getRevenueByBrand(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        
        List<BrandRevenueResponse> data = dashboardService.getRevenueByBrand(start, end);
        return ResponseEntity.ok(ApiResponse.success("Lấy doanh thu theo thương hiệu thành công", data));
    }

    @GetMapping("/top-selling")
    public ResponseEntity<ApiResponse<List<TopSellingResponse>>> getTopSelling(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        
        List<TopSellingResponse> data = dashboardService.getTopSellingProducts(start, end, limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm bán chạy thành công", data));
    }

    @GetMapping("/inventory-value")
    public ResponseEntity<ApiResponse<InventoryValueResponse>> getInventoryValue() {
        InventoryValueResponse data = dashboardService.getInventoryValue();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê giá trị tồn kho thành công", data));
    }

    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getKpis() {
        java.util.Map<String, Object> data = dashboardService.getKpis();
        return ResponseEntity.ok(ApiResponse.success("Lấy KPIs thành công", data));
    }

    @GetMapping("/recent-orders")
    public ResponseEntity<ApiResponse<List<Object>>> getRecentOrders() {
        List<Object> data = dashboardService.getRecentOrders();
        return ResponseEntity.ok(ApiResponse.success("Lấy đơn hàng gần đây thành công", data));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<Object>>> getAlerts() {
        List<Object> data = dashboardService.getAlerts();
        return ResponseEntity.ok(ApiResponse.success("Lấy cảnh báo (alerts) thành công", data));
    }
}
