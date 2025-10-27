package com.example.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for monthly revenue data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenueResponse {
    private Integer year;
    private Integer month;
    private String monthName;
    private BigDecimal totalRevenue;
    private Integer orderCount;
    private Integer successfulPayments;
    private BigDecimal averageOrderValue;
    private BigDecimal growthPercentage; // Compared to previous month
}
