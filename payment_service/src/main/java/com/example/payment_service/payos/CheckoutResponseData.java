package com.example.payment_service.payos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CheckoutResponseData - Response khi tạo payment link
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseData {
    private String bin;
    private String accountNumber;
    private String accountName;
    private Integer amount;
    private String description;
    private Long orderCode;
    private String currency;
    private String paymentLinkId;
    private String status;
    private String checkoutUrl;
    private String qrCode;
}