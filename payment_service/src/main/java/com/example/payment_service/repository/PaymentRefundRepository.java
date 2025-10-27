package com.example.payment_service.repository;

import com.example.payment_service.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
    Optional<PaymentRefund> findByRefundCode(String refundCode);
    List<PaymentRefund> findByPaymentId(Long paymentId);
    List<PaymentRefund> findByStatus(String status);
}
