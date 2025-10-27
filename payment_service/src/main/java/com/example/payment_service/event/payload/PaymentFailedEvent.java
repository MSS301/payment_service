package com.example.payment_service.event.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    @JsonProperty("payment_id")
    private Long paymentId;
    
    @JsonProperty("order_id")
    private Long orderId;
    
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
