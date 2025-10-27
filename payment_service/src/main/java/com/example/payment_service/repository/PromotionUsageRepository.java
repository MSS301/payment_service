package com.example.payment_service.repository;

import com.example.payment_service.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {
    List<PromotionUsage> findByPromotionId(Long promotionId);
    List<PromotionUsage> findByUserId(Long userId);
    int countByPromotionIdAndUserId(Long promotionId, Long userId);
}
