package com.example.payment_service.service;

import com.example.payment_service.entity.Promotion;
import com.example.payment_service.entity.PromotionUsage;
import com.example.payment_service.entity.PaymentOrder;
import com.example.payment_service.repository.PromotionRepository;
import com.example.payment_service.repository.PromotionUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PromotionService {
    
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository usageRepository;
    
    /**
     * Validate and apply promotion code to an order
     */
    public BigDecimal applyPromotion(String promotionCode, String userId, BigDecimal orderAmount) {
        Promotion promotion = promotionRepository.findByCode(promotionCode)
                .orElseThrow(() -> new RuntimeException("Promotion not found: " + promotionCode));
        
        // Validate promotion
        validatePromotion(promotion, userId, orderAmount);
        
        // Calculate discount
        BigDecimal discountAmount = calculateDiscount(promotion, orderAmount);
        
        log.info("Promotion applied: code={}, userId={}, discount={}", 
                promotionCode, userId, discountAmount);
        
        return discountAmount;
    }
    
    /**
     * Record promotion usage
     */
    public PromotionUsage recordPromotionUsage(String promotionCode, String userId,
                                                 PaymentOrder order, BigDecimal discountAmount) {
        Promotion promotion = promotionRepository.findByCode(promotionCode)
                .orElseThrow(() -> new RuntimeException("Promotion not found: " + promotionCode));
        
        PromotionUsage usage = PromotionUsage.builder()
                .promotion(promotion)
                .userId(userId)
                .order(order)
                .discountAmount(discountAmount)
                .build();
        
        usage = usageRepository.save(usage);
        
        // Update promotion usage count
        promotion.setCurrentUses(promotion.getCurrentUses() + 1);
        promotionRepository.save(promotion);
        
        log.info("Promotion usage recorded: code={}, userId={}, orderId={}", 
                promotionCode, userId, order.getId());
        
        return usage;
    }
    
    /**
     * Validate promotion
     */
    private void validatePromotion(Promotion promotion, String userId, BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check if active
        if (!promotion.getIsActive()) {
            throw new IllegalStateException("Promotion is not active");
        }
        
        // Check validity period
        if (now.isBefore(promotion.getValidFrom()) || now.isAfter(promotion.getValidTo())) {
            throw new IllegalStateException("Promotion is not valid at this time");
        }
        
        // Check minimum amount
        if (promotion.getMinAmount() != null && orderAmount.compareTo(promotion.getMinAmount()) < 0) {
            throw new IllegalStateException("Order amount does not meet minimum requirement");
        }
        
        // Check max uses
        if (promotion.getMaxUses() != null && promotion.getCurrentUses() >= promotion.getMaxUses()) {
            throw new IllegalStateException("Promotion has reached maximum usage limit");
        }
        
        // Check user usage limit
        int userUsageCount = usageRepository.countByPromotionIdAndUserId(promotion.getId(), userId);
        if (promotion.getMaxUsesPerUser() != null && userUsageCount >= promotion.getMaxUsesPerUser()) {
            throw new IllegalStateException("You have reached the usage limit for this promotion");
        }
    }
    
    /**
     * Calculate discount amount
     */
    private BigDecimal calculateDiscount(Promotion promotion, BigDecimal orderAmount) {
        BigDecimal discount;
        
        switch (promotion.getDiscountType()) {
            case "PERCENTAGE":
                discount = orderAmount.multiply(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(100));
                break;
            case "FIXED":
                discount = promotion.getDiscountValue();
                break;
            default:
                discount = BigDecimal.ZERO;
        }
        
        // Apply max discount limit if specified
        if (promotion.getMaxDiscount() != null && discount.compareTo(promotion.getMaxDiscount()) > 0) {
            discount = promotion.getMaxDiscount();
        }
        
        return discount;
    }
    
    /**
     * Get active promotions
     */
    public List<Promotion> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findByIsActiveTrueAndValidFromBeforeAndValidToAfter(now, now);
    }
    
    /**
     * Get promotion by code
     */
    public Promotion getPromotionByCode(String code) {
        return promotionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Promotion not found: " + code));
    }
    
    /**
     * Create new promotion
     */
    public Promotion createPromotion(Promotion promotion) {
        return promotionRepository.save(promotion);
    }
    
    /**
     * Get user's promotion usage history
     */
    public List<PromotionUsage> getUserPromotionUsages(String userId) {
        return usageRepository.findByUserId(userId);
    }
}
