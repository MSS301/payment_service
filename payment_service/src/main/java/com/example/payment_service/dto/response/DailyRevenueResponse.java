package com.example.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for daily revenue data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenueResponse {
    private LocalDate date;
    private BigDecimal totalRevenue;
    private Integer orderCount;
    private Integer successfulPayments;
    private Integer failedPayments;
    private BigDecimal averageOrderValue;
}
