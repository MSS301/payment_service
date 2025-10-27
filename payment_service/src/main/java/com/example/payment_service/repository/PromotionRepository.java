package com.example.payment_service.repository;

import com.example.payment_service.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCode(String code);
    List<Promotion> findByIsActiveTrueAndValidFromBeforeAndValidToAfter(LocalDateTime now1, LocalDateTime now2);
}
