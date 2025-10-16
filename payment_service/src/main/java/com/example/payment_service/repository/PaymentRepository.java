package com.example.payment_service.repository;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByOrderCode(Long orderCode);
    
    List<Payment> findByUserId(String userId);
    
    Page<Payment> findByUserId(String userId, Pageable pageable);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    Optional<Payment> findByReferenceCode(String referenceCode);
    
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.status = :status")
    List<Payment> findUserPaymentsByStatus(@Param("userId") String userId, @Param("status") PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findPaymentsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}