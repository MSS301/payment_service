package com.example.payment_service.event.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment completed event - published when payment is successfully processed
 * Structure matches wallet_service expectations for proper event consumption
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    @JsonProperty("payment_id")
    private Long paymentId;  // Changed to String to match wallet_service
    @JsonProperty("order_id")
    private Long orderId;
    @JsonProperty("user_id")
    private Long userId;     // Changed to String to match wallet_service
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("payment_method")
    private String paymentMethod;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
