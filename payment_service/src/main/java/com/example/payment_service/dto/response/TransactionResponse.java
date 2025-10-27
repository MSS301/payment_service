package com.example.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for payment transaction data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String transactionCode;
    private String providerTransactionId;
    private Long userId;
    private Long orderId;
    private String orderCode;
    private BigDecimal amount;
    private String currency;
    private String status; // PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED
    private String paymentMethod;
    private String paymentProvider;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String failureReason;
}
