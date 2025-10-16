package com.example.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusChangedEvent {
    private Long paymentId;
    private Long orderCode;
    private String userId;
    private String oldStatus;
    private String newStatus;
    private BigDecimal amount;
}