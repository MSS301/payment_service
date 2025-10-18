package com.example.payment_service.payos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebhookData - Dữ liệu chi tiết trong webhook
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookData {
    private Long orderCode;
    private Integer amount;
    private String description;
    private String accountNumber;
    private String reference;
    private String transactionDateTime;
    private String currency;
    private String paymentLinkId;
    private String code;
    private String desc;
    private String counterAccountBankId;
    private String counterAccountBankName;
    private String counterAccountName;
    private String counterAccountNumber;
    private String virtualAccountName;
    private String virtualAccountNumber;
}