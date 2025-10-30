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
public class OrderCreatedEvent {
    @JsonProperty("order_id")
    private Long orderId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("package_id")
    private Long packageId;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
