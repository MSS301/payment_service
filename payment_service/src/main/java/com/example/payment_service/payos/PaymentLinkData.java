package com.example.payment_service.payos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PaymentLinkData - Thông tin chi tiết payment link
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkData {
    private String id;
    private Long orderCode;
    private String checkoutUrl;
    private Integer amount;
    private Integer amountPaid;
    private Integer amountRemaining;
    private String status;
    private String createdAt;
    private List<Transaction> transactions;
    private String cancellationReason;
    private String canceledAt;
}