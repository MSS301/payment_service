package com.example.payment_service.controller;

import com.example.payment_service.dto.request.ValidatePromotionRequest;
import com.example.payment_service.dto.response.PromotionResponse;
import com.example.payment_service.dto.response.PromotionValidationResponse;
import com.example.payment_service.entity.Promotion;
import com.example.payment_service.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for promotion operations
 * Handles promotion validation and listing
 */
@RestController
@RequestMapping("/api/payments/promotions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Promotions", description = "Promotion and discount code management APIs")
public class PromotionController {
    
    private final PromotionService promotionService;
    
    @PostMapping("/{code}/validate")
    @Operation(summary = "Validate promotion code", description = "Validate a promotion code for a specific amount")
    public ResponseEntity<PromotionValidationResponse> validatePromotion(
            @Parameter(description = "Promotion code to validate")
            @PathVariable String code,
            @Valid @RequestBody ValidatePromotionRequest request,
            @Parameter(description = "User ID from authentication token")
            @RequestHeader(value = "X-User-ID", required = false) String userId
    ) {
        log.info("Validating promotion: {} for user: {}, amount: {}", code, userId, request.getAmount());
        
        try {
            BigDecimal discount = promotionService.applyPromotion(code, userId, request.getAmount());
            Promotion promotion = promotionService.getPromotionByCode(code);
            
            return ResponseEntity.ok(PromotionValidationResponse.builder()
                    .valid(true)
                    .message("Promotion is valid")
                    .discountAmount(discount)
                    .bonusCredits(null) // TODO: Add bonus credits logic if needed
                    .promotionCode(code)
                    .promotionName(promotion.getName())
                    .build());
        } catch (Exception e) {
            log.warn("Promotion validation failed: {}", e.getMessage());
            return ResponseEntity.ok(PromotionValidationResponse.builder()
                    .valid(false)
                    .message(e.getMessage())
                    .discountAmount(BigDecimal.ZERO)
                    .bonusCredits(null)
                    .promotionCode(code)
                    .promotionName(null)
                    .build());
        }
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active promotions", description = "Retrieve all currently active promotions")
    public ResponseEntity<List<PromotionResponse>> getActivePromotions() {
        log.info("Getting active promotions");
        
        List<Promotion> promotions = promotionService.getActivePromotions();
        List<PromotionResponse> responses = promotions.stream()
                .map(p -> PromotionResponse.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .description(p.getDescription())
                        .discountType(p.getDiscountType())
                        .discountValue(p.getDiscountValue())
                        .minOrderAmount(p.getMinAmount())
                        .maxDiscountAmount(p.getMaxDiscount())
                        .startDate(p.getCreatedAt())
                        .usageLimit(p.getMaxUses())
                        .usageCount(p.getCurrentUses())
                        .active(p.getIsActive())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{code}")
    @Operation(summary = "Get promotion by code", description = "Retrieve promotion details by code")
    public ResponseEntity<PromotionResponse> getPromotionByCode(
            @Parameter(description = "Promotion code")
            @PathVariable String code
    ) {
        log.info("Getting promotion details for code: {}", code);
        
        Promotion promotion = promotionService.getPromotionByCode(code);
        PromotionResponse response = PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .name(promotion.getName())
                .description(promotion.getDescription())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .minOrderAmount(promotion.getMinAmount())
                .maxDiscountAmount(promotion.getMaxDiscount())
                .startDate(promotion.getCreatedAt())
                .usageLimit(promotion.getMaxUses())
                .usageCount(promotion.getCurrentUses())
                .active(promotion.getIsActive())
                .build();
        
        return ResponseEntity.ok(response);
    }
}
