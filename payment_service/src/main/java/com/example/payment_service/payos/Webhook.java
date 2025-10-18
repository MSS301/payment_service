package com.example.payment_service.payos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Webhook - Dữ liệu webhook từ PayOS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Webhook {
    private String code;
    private String desc;
    private Boolean success;
    private WebhookData data;
    private String signature;
}