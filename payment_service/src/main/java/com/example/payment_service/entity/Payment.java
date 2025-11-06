package com.example.payment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private PaymentOrder order;
    
    @Column(name = "transaction_code", unique = true, nullable = false, length = 100)
    private String transactionCode;
    
    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 10)
    private String currency = "VND";
    
    @Column(name = "status", length = 50)
    private String status = "PENDING"; // PENDING / PROCESSING / SUCCESS / FAILED / CANCELLED / REFUNDED
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}