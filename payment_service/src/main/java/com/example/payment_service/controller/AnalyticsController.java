package com.example.payment_service.controller;

import com.example.payment_service.dto.response.*;
import com.example.payment_service.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for analytics and reporting (Admin only)
 * Provides revenue statistics, transaction reports, and analytics data
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics (Admin)", description = "Analytics and reporting APIs for administrators")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    @GetMapping("/revenue/daily")
    @Operation(summary = "Get daily revenue", description = "Retrieve daily revenue data for a date range (Admin only)")
    public ResponseEntity<List<DailyRevenueResponse>> getDailyRevenue(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Admin ID from authentication token")
            @RequestHeader(value = "X-Admin-ID", required = false) Long adminId
    ) {
        adminId = adminId != null ? adminId : 1L; // TODO: Get from JWT auth + verify admin role
        log.info("Admin {} requesting daily revenue from {} to {}", adminId, from, to);
        
        List<DailyRevenueResponse> revenue = analyticsService.getDailyRevenue(from, to);
        return ResponseEntity.ok(revenue);
    }
    
    @GetMapping("/revenue/monthly")
    @Operation(summary = "Get monthly revenue", description = "Retrieve monthly revenue data for a specific year (Admin only)")
    public ResponseEntity<List<MonthlyRevenueResponse>> getMonthlyRevenue(
            @Parameter(description = "Year (e.g., 2025)")
            @RequestParam int year,
            @Parameter(description = "Admin ID from authentication token")
            @RequestHeader(value = "X-Admin-ID", required = false) Long adminId
    ) {
        adminId = adminId != null ? adminId : 1L; // TODO: Get from JWT auth + verify admin role
        log.info("Admin {} requesting monthly revenue for year {}", adminId, year);
        
        List<MonthlyRevenueResponse> revenue = analyticsService.getMonthlyRevenue(year);
        return ResponseEntity.ok(revenue);
    }
    
    @GetMapping("/revenue/stats")
    @Operation(summary = "Get revenue statistics", description = "Retrieve comprehensive revenue statistics (Admin only)")
    public ResponseEntity<RevenueStatsResponse> getRevenueStats(
            @Parameter(description = "Admin ID from authentication token")
            @RequestHeader(value = "X-Admin-ID", required = false) Long adminId
    ) {
        adminId = adminId != null ? adminId : 1L; // TODO: Get from JWT auth + verify admin role
        log.info("Admin {} requesting revenue statistics", adminId);
        
        RevenueStatsResponse stats = analyticsService.getRevenueStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/payments/transactions")
    @Operation(summary = "Get payment transactions", description = "Retrieve payment transactions with filters (Admin only)")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @Parameter(description = "Filter by payment status (PENDING, SUCCESS, FAILED, etc.)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by payment method code (PAYOS, MOMO, VNPAY, etc.)")
            @RequestParam(required = false) String method,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Admin ID from authentication token")
            @RequestHeader(value = "X-Admin-ID", required = false) Long adminId
    ) {
        adminId = adminId != null ? adminId : 1L; // TODO: Get from JWT auth + verify admin role
        log.info("Admin {} requesting transactions with status={}, method={}", adminId, status, method);
        
        Page<TransactionResponse> transactions = analyticsService.getTransactions(
                status, 
                method, 
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard data", description = "Retrieve all key metrics for admin dashboard (Admin only)")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @Parameter(description = "Admin ID from authentication token")
            @RequestHeader(value = "X-Admin-ID", required = false) Long adminId
    ) {
        adminId = adminId != null ? adminId : 1L; // TODO: Get from JWT auth + verify admin role
        log.info("Admin {} requesting dashboard data", adminId);
        
        RevenueStatsResponse stats = analyticsService.getRevenueStats();
        LocalDate today = LocalDate.now();
        List<DailyRevenueResponse> last7Days = analyticsService.getDailyRevenue(
                today.minusDays(7), 
                today
        );
        
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("stats", stats);
        dashboard.put("last7Days", last7Days);
        dashboard.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(dashboard);
    }
}
