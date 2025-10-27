package com.example.payment_service.event.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueRecordedEvent {
    @JsonProperty("date")
    private LocalDate date;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("payment_id")
    private Long paymentId;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
