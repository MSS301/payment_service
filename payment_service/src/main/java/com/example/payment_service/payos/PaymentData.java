package com.example.payment_service.payos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PaymentData - Dữ liệu để tạo link thanh toán
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentData {
    private Long orderCode;
    private Integer amount;
    private String description;
    private List<ItemData> items;
    private String cancelUrl;
    private String returnUrl;
    private String buyerName;
    private String buyerEmail;
    private String buyerPhone;
    private String buyerAddress;
    private Integer expiredAt;
}