package com.example.payment_service.repository;

import com.example.payment_service.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderCode(String orderCode);
    Page<PaymentOrder> findByUserId(Long userId, Pageable pageable);
    Page<PaymentOrder> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
}
