package com.example.payment_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PackageResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer credits;
    private Integer bonusCredits;
    private BigDecimal price;
    private String currency;
    private Boolean isActive;
    private Integer sortOrder;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

