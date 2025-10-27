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
public class BalanceLowEvent {
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("wallet_id")
    private Long walletId;
    
    @JsonProperty("balance")
    private BigDecimal balance;
    
    @JsonProperty("threshold")
    private BigDecimal threshold;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
