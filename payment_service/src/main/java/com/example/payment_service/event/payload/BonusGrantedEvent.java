package com.example.payment_service.event.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bonus granted event - published when bonus credits are granted to user
 * Structure matches wallet_service expectations for proper event consumption
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonusGrantedEvent {
    @JsonProperty("user_id")
    private String userId;     // Changed to String to match wallet_service
    
    @JsonProperty("amount")
    private BigDecimal amount; // Changed to BigDecimal to match wallet_service
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("reference_id")
    private String referenceId; // Changed from orderId to match wallet_service
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
