package com.example.payment_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotNull(message = "Package ID is required")
    private Long packageId;
    
    @NotNull(message = "Payment method code is required")
    private String paymentMethodCode;
    
    private String promotionCode;
    
    private String userId; // Set from authentication context
}
