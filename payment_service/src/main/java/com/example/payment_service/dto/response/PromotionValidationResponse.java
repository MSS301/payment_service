package com.example.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionValidationResponse {
    
    private Boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private Integer bonusCredits;
    private String promotionCode;
    private String promotionName;
}
