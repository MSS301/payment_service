package com.example.payment_service.service;

import com.example.payment_service.dto.response.*;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.PaymentOrder;
import com.example.payment_service.repository.PaymentOrderRepository;
import com.example.payment_service.repository.PaymentRefundRepository;
import com.example.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analytics and reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentOrderRepository orderRepository;
    private final PaymentRefundRepository refundRepository;
    
    /**
     * Get daily revenue for date range
     */
    public List<DailyRevenueResponse> getDailyRevenue(LocalDate fromDate, LocalDate toDate) {
        log.info("Getting daily revenue from {} to {}", fromDate, toDate);
        
        List<DailyRevenueResponse> results = new ArrayList<>();
        LocalDate currentDate = fromDate;
        
        while (!currentDate.isAfter(toDate)) {
            LocalDateTime startOfDay = currentDate.atStartOfDay();
            LocalDateTime endOfDay = currentDate.atTime(LocalTime.MAX);
            
            List<Payment> payments = paymentRepository.findByCreatedAtBetween(startOfDay, endOfDay);
            
            List<Payment> successfulPayments = payments.stream()
                    .filter(p -> "SUCCESS".equals(p.getStatus()))
                    .collect(Collectors.toList());
            
            BigDecimal totalRevenue = successfulPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            int orderCount = payments.size();
            int successCount = successfulPayments.size();
            int failedCount = orderCount - successCount;
            
            BigDecimal avgOrderValue = orderCount > 0 
                    ? totalRevenue.divide(BigDecimal.valueOf(successCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            results.add(DailyRevenueResponse.builder()
                    .date(currentDate)
                    .totalRevenue(totalRevenue)
                    .orderCount(orderCount)
                    .successfulPayments(successCount)
                    .failedPayments(failedCount)
                    .averageOrderValue(avgOrderValue)
                    .build());
            
            currentDate = currentDate.plusDays(1);
        }
        
        return results;
    }
    
    /**
     * Get monthly revenue for a year
     */
    public List<MonthlyRevenueResponse> getMonthlyRevenue(int year) {
        log.info("Getting monthly revenue for year {}", year);
        
        List<MonthlyRevenueResponse> results = new ArrayList<>();
        BigDecimal previousMonthRevenue = BigDecimal.ZERO;
        
        for (int month = 1; month <= 12; month++) {
            LocalDateTime startOfMonth = LocalDate.of(year, month, 1).atStartOfDay();
            LocalDateTime endOfMonth = startOfMonth.toLocalDate()
                    .withDayOfMonth(startOfMonth.toLocalDate().lengthOfMonth())
                    .atTime(LocalTime.MAX);
            
            List<Payment> payments = paymentRepository.findByCreatedAtBetween(startOfMonth, endOfMonth);
            
            List<Payment> successfulPayments = payments.stream()
                    .filter(p -> "SUCCESS".equals(p.getStatus()))
                    .collect(Collectors.toList());
            
            BigDecimal totalRevenue = successfulPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            int orderCount = payments.size();
            int successCount = successfulPayments.size();
            
            BigDecimal avgOrderValue = successCount > 0 
                    ? totalRevenue.divide(BigDecimal.valueOf(successCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            // Calculate growth percentage
            BigDecimal growthPercentage = BigDecimal.ZERO;
            if (previousMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
                growthPercentage = totalRevenue.subtract(previousMonthRevenue)
                        .divide(previousMonthRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            results.add(MonthlyRevenueResponse.builder()
                    .year(year)
                    .month(month)
                    .monthName(Month.of(month).name())
                    .totalRevenue(totalRevenue)
                    .orderCount(orderCount)
                    .successfulPayments(successCount)
                    .averageOrderValue(avgOrderValue)
                    .growthPercentage(growthPercentage)
                    .build());
            
            previousMonthRevenue = totalRevenue;
        }
        
        return results;
    }
    
    /**
     * Get revenue statistics
     */
    public RevenueStatsResponse getRevenueStats() {
        log.info("Getting revenue statistics");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.minusWeeks(1);
        LocalDateTime startOfMonth = now.minusMonths(1);
        LocalDateTime startOfYear = now.minusYears(1);
        
        // Today stats
        List<Payment> todayPayments = paymentRepository.findByCreatedAtBetween(startOfToday, now);
        BigDecimal todayRevenue = calculateRevenue(todayPayments);
        long todayTransactions = todayPayments.size();
        
        // Week stats
        List<Payment> weekPayments = paymentRepository.findByCreatedAtBetween(startOfWeek, now);
        BigDecimal weekRevenue = calculateRevenue(weekPayments);
        long weekTransactions = weekPayments.size();
        
        // Month stats
        List<Payment> monthPayments = paymentRepository.findByCreatedAtBetween(startOfMonth, now);
        BigDecimal monthRevenue = calculateRevenue(monthPayments);
        long monthTransactions = monthPayments.size();
        
        // Year stats
        List<Payment> yearPayments = paymentRepository.findByCreatedAtBetween(startOfYear, now);
        BigDecimal yearRevenue = calculateRevenue(yearPayments);
        long yearTransactions = yearPayments.size();
        
        // Average transaction value
        BigDecimal avgTransactionValue = yearTransactions > 0
                ? yearRevenue.divide(BigDecimal.valueOf(yearTransactions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        // Total refunds
        BigDecimal totalRefunds = refundRepository.findAll().stream()
                .map(r -> r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        

        
        // Transactions by status
        Map<String, Long> transactionsByStatus = yearPayments.stream()
                .collect(Collectors.groupingBy(
                        Payment::getStatus,
                        Collectors.counting()
                ));
        
        return RevenueStatsResponse.builder()
                .todayRevenue(todayRevenue)
                .weekRevenue(weekRevenue)
                .monthRevenue(monthRevenue)
                .yearRevenue(yearRevenue)
                .todayTransactions(todayTransactions)
                .weekTransactions(weekTransactions)
                .monthTransactions(monthTransactions)
                .yearTransactions(yearTransactions)
                .averageTransactionValue(avgTransactionValue)
                .totalRefunds(totalRefunds)
                .transactionsByStatus(transactionsByStatus)
                .build();
    }
    
    /**
     * Get payment transactions with filters
     */
    public Page<TransactionResponse> getTransactions(String status, String methodCode, Pageable pageable) {
        log.info("Getting transactions with status: {}, method: {}", status, methodCode);
        
        Page<Payment> payments;
                if (status != null) {
                        // Filter by status only (paymentMethod not present on Payment entity)
                        payments = paymentRepository.findByStatus(status, pageable);
                } else {
                        // No status filter available; ignore methodCode since Payment has no paymentMethod relation
                        payments = paymentRepository.findAll(pageable);
                }
        
        return payments.map(this::toTransactionResponse);
    }
    
    /**
     * Calculate total revenue from payments
     */
    private BigDecimal calculateRevenue(List<Payment> payments) {
        return payments.stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Convert Payment entity to TransactionResponse
     */
    private TransactionResponse toTransactionResponse(Payment payment) {
        return TransactionResponse.builder()
                .id(payment.getId())
                .transactionCode(payment.getTransactionCode())
                .providerTransactionId(payment.getProviderTransactionId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .orderCode(payment.getOrder() != null ? payment.getOrder().getOrderCode() : null)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
