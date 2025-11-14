package com.example.payment_service.controller;

import com.example.payment_service.dto.ApiResponse;
import com.example.payment_service.dto.response.DailyRevenueResponse;
import com.example.payment_service.dto.response.PaymentByStatusResponse;
import com.example.payment_service.dto.response.PaymentOverviewResponse;
import com.example.payment_service.dto.response.RevenueStatsResponse;
import com.example.payment_service.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for dashboard analytics and statistics
 * Provides endpoints for revenue tracking, payment overview, and status-based analytics
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Dashboard analytics and statistics APIs")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get revenue statistics
     * Returns comprehensive revenue data including daily, weekly, monthly, and yearly metrics
     */
    @GetMapping("/revenue")
    @Operation(summary = "Get revenue statistics",
               description = "Retrieve comprehensive revenue statistics with breakdown by time periods")
    public ResponseEntity<ApiResponse<RevenueStatsResponse>> getRevenueStats(
            @Parameter(description = "Start date for filtering (optional)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,

            @Parameter(description = "End date for filtering (optional)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate
    ) {
        log.info("Getting revenue stats with startDate: {}, endDate: {}", startDate, endDate);

        RevenueStatsResponse stats = dashboardService.getRevenueStats(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.<RevenueStatsResponse>builder()
                .code(1000)
                .message("Revenue statistics retrieved successfully")
                .result(stats)
                .build());
    }

    /**
     * Get payment overview
     * Returns summary of all payments including counts and amounts by status
     */
    @GetMapping("/payment-overview")
    @Operation(summary = "Get payment overview",
               description = "Retrieve comprehensive payment statistics including totals, success rates, and period comparisons")
    public ResponseEntity<ApiResponse<PaymentOverviewResponse>> getPaymentOverview() {
        log.info("Getting payment overview");

        PaymentOverviewResponse overview = dashboardService.getPaymentOverview();

        return ResponseEntity.ok(ApiResponse.<PaymentOverviewResponse>builder()
                .code(1000)
                .message("Payment overview retrieved successfully")
                .result(overview)
                .build());
    }

    /**
     * Get payment statistics grouped by status
     * Returns count, amount, and percentage for each payment status
     */
    @GetMapping("/payment-by-status")
    @Operation(summary = "Get payments by status",
               description = "Retrieve payment statistics grouped by status with counts, amounts, and percentages")
    public ResponseEntity<ApiResponse<List<PaymentByStatusResponse>>> getPaymentsByStatus() {
        log.info("Getting payments by status");

        List<PaymentByStatusResponse> paymentsByStatus = dashboardService.getPaymentsByStatus();

        return ResponseEntity.ok(ApiResponse.<List<PaymentByStatusResponse>>builder()
                .code(1000)
                .message("Payment status statistics retrieved successfully")
                .result(paymentsByStatus)
                .build());
    }

    /**
     * Get daily revenue for date range
     * Returns day-by-day revenue breakdown
     */
    @GetMapping("/daily-revenue")
    @Operation(summary = "Get daily revenue",
               description = "Retrieve daily revenue breakdown for a specified date range")
    public ResponseEntity<ApiResponse<List<DailyRevenueResponse>>> getDailyRevenue(
            @Parameter(description = "Start date (defaults to 30 days ago)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(description = "End date (defaults to today)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        log.info("Getting daily revenue from {} to {}", startDate, endDate);

        List<DailyRevenueResponse> dailyRevenue = dashboardService.getDailyRevenue(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.<List<DailyRevenueResponse>>builder()
                .code(1000)
                .message("Daily revenue retrieved successfully")
                .result(dailyRevenue)
                .build());
    }
}

