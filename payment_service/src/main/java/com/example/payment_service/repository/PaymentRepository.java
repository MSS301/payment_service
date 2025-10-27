package com.example.payment_service.repository;

import com.example.payment_service.entity.Payment;
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
    
    Optional<Payment> findByTransactionCode(String transactionCode);
    
    Optional<Payment> findByProviderTransactionId(String providerTransactionId);
    
    List<Payment> findByOrderId(Long orderId);
    
    List<Payment> findByStatus(String status);
    
    Page<Payment> findByStatus(String status, Pageable pageable);
    
    
    List<Payment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT p FROM Payment p WHERE p.order.userId = :userId AND p.status = :status")
    List<Payment> findUserPaymentsByStatus(@Param("userId") Long userId, @Param("status") String status);
    
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findPaymentsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
