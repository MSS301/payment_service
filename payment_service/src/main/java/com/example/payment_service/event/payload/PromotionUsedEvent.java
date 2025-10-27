package com.example.payment_service.event.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionUsedEvent {
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("promotion_code")
    private String promotionCode;
    
    @JsonProperty("discount")
    private BigDecimal discount;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
