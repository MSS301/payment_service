package com.example.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    
    private Long id;
    private Long paymentId;
    private String refundCode;
    private String providerRefundId;
    
    private BigDecimal amount;
    private String reason;
    private String status;
    
    private String initiatedBy;
    
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
