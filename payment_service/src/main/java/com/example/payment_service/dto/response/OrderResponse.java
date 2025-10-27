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
public class OrderResponse {
    
    private Long orderId;
    private String orderCode;
    private Long userId;
    private Long packageId;
    private String packageName;
    private Integer credits;
    private Integer bonusCredits;
    
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String currency;
    
    private String status;
    private String description;
    
    private String paymentUrl;
    private LocalDateTime expiresAt;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
