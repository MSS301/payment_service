package com.example.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for payment statistics grouped by status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentByStatusResponse {

    private String status;
    private Long count;
    private BigDecimal totalAmount;
    private Double percentage; // Percentage of total payments
}

