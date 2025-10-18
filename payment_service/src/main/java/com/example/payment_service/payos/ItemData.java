package com.example.payment_service.payos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ItemData - Thông tin sản phẩm
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemData {
    private String name;
    private Integer quantity;
    private Integer price;
}