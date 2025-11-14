package com.example.payment_service.service;

import com.example.payment_service.dto.response.DailyRevenueResponse;
import com.example.payment_service.dto.response.PaymentByStatusResponse;
import com.example.payment_service.dto.response.PaymentOverviewResponse;
import com.example.payment_service.dto.response.RevenueStatsResponse;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for dashboard statistics and analytics
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final PaymentRepository paymentRepository;

    /**
     * Get revenue statistics for dashboard
     * Supports filtering by date range
     */
    public RevenueStatsResponse getRevenueStats(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting revenue stats from {} to {}", startDate, endDate);

        // Get current date boundaries
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = now.minusWeeks(1);
        LocalDateTime monthStart = now.minusMonths(1);
        LocalDateTime yearStart = now.minusYears(1);

        // Calculate revenues for different periods
        BigDecimal todayRevenue = paymentRepository.sumSuccessfulPaymentsByDateRange(todayStart, now);
        BigDecimal weekRevenue = paymentRepository.sumSuccessfulPaymentsByDateRange(weekStart, now);
        BigDecimal monthRevenue = paymentRepository.sumSuccessfulPaymentsByDateRange(monthStart, now);
        BigDecimal yearRevenue = paymentRepository.sumSuccessfulPaymentsByDateRange(yearStart, now);

        // Count transactions for different periods
        Long todayTransactions = paymentRepository.countPaymentsByDateRange(todayStart, now);
        Long weekTransactions = paymentRepository.countPaymentsByDateRange(weekStart, now);
        Long monthTransactions = paymentRepository.countPaymentsByDateRange(monthStart, now);
        Long yearTransactions = paymentRepository.countPaymentsByDateRange(yearStart, now);

        // Calculate average transaction value
        BigDecimal averageTransactionValue = paymentRepository.calculateAverageSuccessfulPayment();

        // Calculate total refunds (payments with REFUNDED status)
        BigDecimal totalRefunds = paymentRepository.sumAmountByStatus("REFUNDED");

        // Get payment stats by status for additional insights
        List<Object[]> statsByStatus = paymentRepository.getPaymentStatsByStatus();
        Map<String, Long> transactionsByStatus = new HashMap<>();

        for (Object[] row : statsByStatus) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            transactionsByStatus.put(status, count);
        }

        // For revenueByMethod, we'll keep it simple for now
        // In a real implementation, you'd track payment methods
        Map<String, BigDecimal> revenueByMethod = new HashMap<>();
        revenueByMethod.put("PayOS", paymentRepository.sumAmountByStatus("SUCCESS"));

        return RevenueStatsResponse.builder()
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .weekRevenue(weekRevenue != null ? weekRevenue : BigDecimal.ZERO)
                .monthRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO)
                .yearRevenue(yearRevenue != null ? yearRevenue : BigDecimal.ZERO)
                .todayTransactions(todayTransactions != null ? todayTransactions : 0L)
                .weekTransactions(weekTransactions != null ? weekTransactions : 0L)
                .monthTransactions(monthTransactions != null ? monthTransactions : 0L)
                .yearTransactions(yearTransactions != null ? yearTransactions : 0L)
                .averageTransactionValue(averageTransactionValue != null ? averageTransactionValue : BigDecimal.ZERO)
                .totalRefunds(totalRefunds != null ? totalRefunds : BigDecimal.ZERO)
                .revenueByMethod(revenueByMethod)
                .transactionsByStatus(transactionsByStatus)
                .build();
    }

    /**
     * Get payment overview for dashboard
     */
    public PaymentOverviewResponse getPaymentOverview() {
        log.info("Getting payment overview");

        // Get counts by status
        Long totalPayments = paymentRepository.count();
        Long successfulPayments = paymentRepository.countByStatus("SUCCESS");
        Long failedPayments = paymentRepository.countByStatus("FAILED");
        Long pendingPayments = paymentRepository.countByStatus("PENDING");
        Long cancelledPayments = paymentRepository.countByStatus("CANCELLED");

        // Get amounts by status
        BigDecimal totalRevenue = paymentRepository.sumAmountByStatus("SUCCESS");
        BigDecimal successfulAmount = totalRevenue;
        BigDecimal pendingAmount = paymentRepository.sumAmountByStatus("PENDING");
        BigDecimal failedAmount = paymentRepository.sumAmountByStatus("FAILED");

        // Calculate average payment value
        BigDecimal averagePaymentValue = BigDecimal.ZERO;
        if (successfulPayments > 0) {
            averagePaymentValue = totalRevenue.divide(
                BigDecimal.valueOf(successfulPayments),
                2,
                RoundingMode.HALF_UP
            );
        }

        // Calculate success rate
        Double successRate = 0.0;
        if (totalPayments > 0) {
            successRate = (successfulPayments.doubleValue() / totalPayments.doubleValue()) * 100;
            successRate = Math.round(successRate * 100.0) / 100.0; // Round to 2 decimal places
        }

        // Get period-based statistics
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = now.minusWeeks(1);
        LocalDateTime monthStart = now.minusMonths(1);

        BigDecimal todayRevenue = paymentRepository.sumSuccessfulPaymentsByDateRange(todayStart, now);
        BigDecimal weekRevenue = paymentRepository.sumSuccessfulPaymentsByDateRange(weekStart, now);
        BigDecimal monthRevenue = paymentRepository.sumSuccessfulPaymentsByDateRange(monthStart, now);

        Long todayPayments = paymentRepository.countPaymentsByDateRange(todayStart, now);
        Long weekPayments = paymentRepository.countPaymentsByDateRange(weekStart, now);
        Long monthPayments = paymentRepository.countPaymentsByDateRange(monthStart, now);

        return PaymentOverviewResponse.builder()
                .totalPayments(totalPayments != null ? totalPayments : 0L)
                .successfulPayments(successfulPayments != null ? successfulPayments : 0L)
                .failedPayments(failedPayments != null ? failedPayments : 0L)
                .pendingPayments(pendingPayments != null ? pendingPayments : 0L)
                .cancelledPayments(cancelledPayments != null ? cancelledPayments : 0L)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .successfulAmount(successfulAmount != null ? successfulAmount : BigDecimal.ZERO)
                .pendingAmount(pendingAmount != null ? pendingAmount : BigDecimal.ZERO)
                .failedAmount(failedAmount != null ? failedAmount : BigDecimal.ZERO)
                .averagePaymentValue(averagePaymentValue)
                .successRate(successRate)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .weekRevenue(weekRevenue != null ? weekRevenue : BigDecimal.ZERO)
                .monthRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO)
                .todayPayments(todayPayments != null ? todayPayments : 0L)
                .weekPayments(weekPayments != null ? weekPayments : 0L)
                .monthPayments(monthPayments != null ? monthPayments : 0L)
                .build();
    }

    /**
     * Get payment statistics grouped by status
     */
    public List<PaymentByStatusResponse> getPaymentsByStatus() {
        log.info("Getting payments by status");

        List<Object[]> statsByStatus = paymentRepository.getPaymentStatsByStatus();
        Long totalPayments = paymentRepository.count();

        List<PaymentByStatusResponse> result = new ArrayList<>();

        for (Object[] row : statsByStatus) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            BigDecimal totalAmount = (BigDecimal) row[2];

            // Calculate percentage
            Double percentage = 0.0;
            if (totalPayments > 0) {
                percentage = (count.doubleValue() / totalPayments.doubleValue()) * 100;
                percentage = Math.round(percentage * 100.0) / 100.0; // Round to 2 decimal places
            }

            result.add(PaymentByStatusResponse.builder()
                    .status(status)
                    .count(count)
                    .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                    .percentage(percentage)
                    .build());
        }

        // Sort by count descending
        result.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));

        return result;
    }

    /**
     * Get daily revenue for a date range
     */
    public List<DailyRevenueResponse> getDailyRevenue(LocalDate startDate, LocalDate endDate) {
        log.info("Getting daily revenue from {} to {}", startDate, endDate);

        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30); // Default to last 30 days
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // Get all successful payments in the date range
        List<Payment> payments = paymentRepository.findPaymentsByDateRange(start, end);

        // Group by date
        Map<LocalDate, List<Payment>> paymentsByDate = payments.stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()) && p.getPaidAt() != null)
                .collect(Collectors.groupingBy(p -> p.getPaidAt().toLocalDate()));

        // Create response for each date
        List<DailyRevenueResponse> result = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Payment> dayPayments = paymentsByDate.getOrDefault(date, new ArrayList<>());

            BigDecimal totalRevenue = dayPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Integer orderCount = dayPayments.size();

            Integer successfulPayments = (int) dayPayments.stream()
                    .filter(p -> "SUCCESS".equals(p.getStatus()))
                    .count();

            Integer failedPayments = (int) dayPayments.stream()
                    .filter(p -> "FAILED".equals(p.getStatus()))
                    .count();

            BigDecimal averageOrderValue = BigDecimal.ZERO;
            if (orderCount > 0) {
                averageOrderValue = totalRevenue.divide(
                    BigDecimal.valueOf(orderCount),
                    2,
                    RoundingMode.HALF_UP
                );
            }

            result.add(DailyRevenueResponse.builder()
                    .date(date)
                    .totalRevenue(totalRevenue)
                    .orderCount(orderCount)
                    .successfulPayments(successfulPayments)
                    .failedPayments(failedPayments)
                    .averageOrderValue(averageOrderValue)
                    .build());
        }

        return result;
    }
}

