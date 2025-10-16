package com.example.payment_service.service;

import com.example.payment_service.dto.request.CreatePaymentRequest;
import com.example.payment_service.dto.response.PaymentResponse;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.PaymentStatus;
import com.example.payment_service.event.PaymentCreatedEvent;
import com.example.payment_service.event.PaymentStatusChangedEvent;
import com.example.payment_service.exception.PaymentNotFoundException;
import com.example.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final PayOS payOS;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${payos.return-url}")
    private String returnUrl;
    
    @Value("${payos.cancel-url}")
    private String cancelUrl;
    
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        try {
            // Generate unique order code
            Long orderCode = generateOrderCode();
            
            // Create payment entity
            Payment payment = Payment.builder()
                    .orderCode(orderCode)
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .status(PaymentStatus.PENDING)
                    .referenceCode(request.getReferenceCode())
                    .build();
            
            // Create PayOS payment data
            ItemData item = ItemData.builder()
                    .name(request.getDescription())
                    .quantity(1)
                    .price(request.getAmount().intValue())
                    .build();
            
            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(request.getAmount().intValue())
                    .description(request.getDescription())
                    .items(List.of(item))
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .build();
            
            // Create payment link with PayOS
            vn.payos.type.CheckoutResponseData checkoutResponse = payOS.createPaymentLink(paymentData);
            payment.setPaymentUrl(checkoutResponse.getCheckoutUrl());
            
            // Save payment
            payment = paymentRepository.save(payment);
            
            // Publish event
            PaymentCreatedEvent event = PaymentCreatedEvent.builder()
                    .paymentId(payment.getId())
                    .orderCode(payment.getOrderCode())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .status(payment.getStatus().name())
                    .build();
            
            kafkaTemplate.send("payment-created", event);
            
            log.info("Payment created successfully: {}", payment.getOrderCode());
            
            return mapToResponse(payment);
            
        } catch (Exception e) {
            log.error("Error creating payment: ", e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage());
        }
    }
    
    public PaymentResponse getPaymentByOrderCode(Long orderCode) {
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with order code: " + orderCode));
        return mapToResponse(payment);
    }
    
    public List<PaymentResponse> getUserPayments(String userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream().map(this::mapToResponse).toList();
    }
    
    public Page<PaymentResponse> getUserPayments(String userId, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findByUserId(userId, pageable);
        return payments.map(this::mapToResponse);
    }
    
    public PaymentResponse updatePaymentStatus(Long orderCode, PaymentStatus newStatus) {
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with order code: " + orderCode));
        
        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(newStatus);
        
        // Update timestamps based on status
        switch (newStatus) {
            case PAID -> payment.setPaidAt(LocalDateTime.now());
            case CANCELLED -> payment.setCancelledAt(LocalDateTime.now());
        }
        
        payment = paymentRepository.save(payment);
        
        // Publish status change event
        PaymentStatusChangedEvent event = PaymentStatusChangedEvent.builder()
                .paymentId(payment.getId())
                .orderCode(payment.getOrderCode())
                .userId(payment.getUserId())
                .oldStatus(oldStatus.name())
                .newStatus(newStatus.name())
                .amount(payment.getAmount())
                .build();
        
        kafkaTemplate.send("payment-status-changed", event);
        
        log.info("Payment status updated: {} from {} to {}", orderCode, oldStatus, newStatus);
        
        return mapToResponse(payment);
    }
    
    public void handlePaymentWebhook(Long orderCode, String status) {
        try {
            Payment payment = paymentRepository.findByOrderCode(orderCode)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found with order code: " + orderCode));
            
            PaymentStatus newStatus = switch (status.toLowerCase()) {
                case "paid" -> PaymentStatus.PAID;
                case "cancelled" -> PaymentStatus.CANCELLED;
                case "expired" -> PaymentStatus.EXPIRED;
                default -> PaymentStatus.FAILED;
            };
            
            if (!payment.getStatus().equals(newStatus)) {
                updatePaymentStatus(orderCode, newStatus);
            }
            
        } catch (Exception e) {
            log.error("Error handling payment webhook for order code {}: ", orderCode, e);
            throw new RuntimeException("Failed to handle payment webhook: " + e.getMessage());
        }
    }
    
    private Long generateOrderCode() {
        Random random = new Random();
        Long orderCode;
        do {
            orderCode = 1000000L + random.nextLong(9000000L);
        } while (paymentRepository.findByOrderCode(orderCode).isPresent());
        return orderCode;
    }
    
    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderCode(payment.getOrderCode())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .description(payment.getDescription())
                .status(payment.getStatus())
                .paymentUrl(payment.getPaymentUrl())
                .referenceCode(payment.getReferenceCode())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .paidAt(payment.getPaidAt())
                .cancelledAt(payment.getCancelledAt())
                .build();
    }
}