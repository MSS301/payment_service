package com.example.payment_service.service;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.PaymentRefund;
import com.example.payment_service.exception.PaymentNotFoundException;
import com.example.payment_service.repository.PaymentRefundRepository;
import com.example.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RefundService {
    
    private final PaymentRefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    
    /**
     * Create a refund request for a payment
     */
    public PaymentRefund createRefund(String transactionCode, String reason, String initiatedBy) {
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + transactionCode));
        
        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new IllegalStateException("Cannot refund payment with status: " + payment.getStatus());
        }
        
        // Generate unique refund code
        String refundCode = "RF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        PaymentRefund refund = PaymentRefund.builder()
                .payment(payment)
                .refundCode(refundCode)
                .amount(payment.getAmount())
                .reason(reason)
                .status("PENDING")
                .initiatedBy(initiatedBy)
                .build();
        
        refund = refundRepository.save(refund);
        
        log.info("Refund created: refundCode={}, paymentId={}, amount={}", 
                refundCode, payment.getId(), payment.getAmount());
        
        return refund;
    }
    
    /**
     * Process a refund (update status)
     */
    public PaymentRefund processRefund(String refundCode, String newStatus, String providerRefundId) {
        PaymentRefund refund = refundRepository.findByRefundCode(refundCode)
                .orElseThrow(() -> new RuntimeException("Refund not found: " + refundCode));
        
        refund.setStatus(newStatus);
        refund.setProviderRefundId(providerRefundId);
        
        if ("SUCCESS".equals(newStatus)) {
            refund.setProcessedAt(LocalDateTime.now());
            
            // Update payment status to REFUNDED
            Payment payment = refund.getPayment();
            payment.setStatus("REFUNDED");
            paymentRepository.save(payment);
        }
        
        refund = refundRepository.save(refund);
        
        log.info("Refund processed: refundCode={}, newStatus={}", refundCode, newStatus);
        
        return refund;
    }
    
    /**
     * Get refund by code
     */
    public PaymentRefund getRefundByCode(String refundCode) {
        return refundRepository.findByRefundCode(refundCode)
                .orElseThrow(() -> new RuntimeException("Refund not found: " + refundCode));
    }
    
    /**
     * Get all refunds for a payment
     */
    public List<PaymentRefund> getRefundsForPayment(Long paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }
    
    /**
     * Get refunds by status
     */
    public List<PaymentRefund> getRefundsByStatus(String status) {
        return refundRepository.findByStatus(status);
    }
    public List<PaymentRefund> getAllRefunds() {
        return refundRepository.findAll();
    }
    public PaymentRefund getRefundById(Long id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Refund not found with id: " + id));
    }
}
