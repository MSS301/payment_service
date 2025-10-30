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
public class PaymentInitiatedEvent {
    @JsonProperty("payment_id")
    private Long paymentId;
    
    @JsonProperty("order_id")
    private Long orderId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("credits")
    private Integer credits;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
