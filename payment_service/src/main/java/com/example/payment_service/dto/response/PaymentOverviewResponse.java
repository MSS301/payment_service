package com.example.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for payment overview dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOverviewResponse {

    // Total statistics
    private Long totalPayments;
    private Long successfulPayments;
    private Long failedPayments;
    private Long pendingPayments;
    private Long cancelledPayments;

    // Amount statistics
    private BigDecimal totalRevenue;
    private BigDecimal successfulAmount;
    private BigDecimal pendingAmount;
    private BigDecimal failedAmount;

    // Averages
    private BigDecimal averagePaymentValue;
    private Double successRate; // Percentage

    // Period comparisons
    private BigDecimal todayRevenue;
    private BigDecimal weekRevenue;
    private BigDecimal monthRevenue;

    private Long todayPayments;
    private Long weekPayments;
    private Long monthPayments;
}

