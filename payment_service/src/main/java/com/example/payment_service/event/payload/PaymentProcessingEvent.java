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
public class PaymentProcessingEvent {
    @JsonProperty("payment_id")
    private Long paymentId;
    
    @JsonProperty("provider")
    private String provider;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
