package com.example.payment_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePackageRequest {

    @Size(max = 100, message = "Package name must not exceed 100 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Min(value = 1, message = "Credits must be at least 1")
    private Integer credits;

    @Min(value = 0, message = "Bonus credits must be at least 0")
    private Integer bonusCredits;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;

    private Boolean isActive;

    private Integer sortOrder;

    private String metadata;
}

