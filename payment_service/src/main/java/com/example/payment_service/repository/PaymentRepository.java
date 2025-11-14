package com.example.payment_service.repository;

import com.example.payment_service.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    // Saga Pattern - Timeout handling
    List<Payment> findByStatusAndUpdatedAtBefore(String status, LocalDateTime cutoffTime);

    List<Payment> findByStatusAndCreatedAtAfter(String status, LocalDateTime after);

    // Dashboard queries
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.paidAt BETWEEN :startDate AND :endDate")
    BigDecimal sumSuccessfulPaymentsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paidAt BETWEEN :startDate AND :endDate")
    Long countPaymentsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(AVG(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal calculateAverageSuccessfulPayment();

    @Query("SELECT p.status as status, COUNT(p) as count, COALESCE(SUM(p.amount), 0) as totalAmount " +
           "FROM Payment p GROUP BY p.status")
    List<Object[]> getPaymentStatsByStatus();
}
