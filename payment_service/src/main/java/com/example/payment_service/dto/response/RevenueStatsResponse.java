package com.example.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatsResponse {
    
    private BigDecimal todayRevenue;
    private BigDecimal weekRevenue;
    private BigDecimal monthRevenue;
    private BigDecimal yearRevenue;
    
    private Long todayTransactions;
    private Long weekTransactions;
    private Long monthTransactions;
    private Long yearTransactions;
    
    private BigDecimal averageTransactionValue;
    private BigDecimal totalRefunds;
    
    private Map<String, BigDecimal> revenueByMethod;
    private Map<String, Long> transactionsByStatus;
}
