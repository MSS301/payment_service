package com.example.payment_service.service;

import com.example.payment_service.dto.request.CreatePaymentRequest;
import com.example.payment_service.dto.response.PaymentResponse;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.PaymentStatus;
import com.example.payment_service.event.payload.PaymentCompletedEvent;
import com.example.payment_service.event.payload.PaymentFailedEvent;
import com.example.payment_service.event.payload.PaymentInitiatedEvent;
import com.example.payment_service.exception.PaymentNotFoundException;
import com.example.payment_service.payos.CheckoutResponseData;
import com.example.payment_service.payos.ItemData;
import com.example.payment_service.payos.PayOS;
import com.example.payment_service.payos.PaymentData;
import com.example.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling payment operations
 * Integrates with PayOS payment gateway and manages payment lifecycle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final PayOS payOS;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${payos.return-url}")
    private String returnUrl;
    
    @Value("${payos.cancel-url}")
    private String cancelUrl;
    
    /**
     * Creates a new payment and generates payment link via PayOS
     * 
     * @param request Payment creation request
     * @return Payment response with payment URL
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        try {
            // Generate unique transaction code
            String transactionCode = generateTransactionCode();
            
            // Create payment entity
            Payment payment = new Payment();
            payment.setTransactionCode(transactionCode);
            payment.setAmount(request.getAmount());
            payment.setCurrency("VND");
            payment.setStatus("PENDING");
            
            // Save payment first to get ID
            payment = paymentRepository.save(payment);
            
            // Create PayOS payment data
            ItemData item = new ItemData();
            item.setName(request.getDescription());
            item.setQuantity(1);
            item.setPrice(request.getAmount().intValue());
            
            // Generate order code from payment ID
            long orderCode = generateOrderCode(payment.getId());
            
            PaymentData paymentData = new PaymentData();
            paymentData.setOrderCode(orderCode);
            paymentData.setAmount(request.getAmount().intValue());
            paymentData.setDescription(request.getDescription());
            paymentData.setItems(List.of(item));
            paymentData.setReturnUrl(returnUrl);
            paymentData.setCancelUrl(cancelUrl);
            
            // Create payment link with PayOS
            CheckoutResponseData checkoutResponse = payOS.createPaymentLink(paymentData);
            
            // Update payment with PayOS response
            payment.setProviderTransactionId(String.valueOf(orderCode));
            payment = paymentRepository.save(payment);
            
            // Publish payment initiated event
            publishPaymentInitiatedEvent(payment, request.getUserId());
            
            log.info("Payment created successfully with transaction code: {}", payment.getTransactionCode());
            
            return mapToResponse(payment, checkoutResponse.getCheckoutUrl());
            
        } catch (Exception e) {
            log.error("Error creating payment for user {}: ", request.getUserId(), e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Retrieves payment by transaction code
     * 
     * @param transactionCode Unique transaction code
     * @return Payment response
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionCode(String transactionCode) {
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with transaction code: " + transactionCode));
        return mapToResponse(payment, null);
    }
    
    /**
     * Retrieves payment by provider transaction ID
     * 
     * @param providerTransactionId Provider's transaction ID
     * @return Payment response
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByProviderTransactionId(String providerTransactionId) {
        Payment payment = paymentRepository.findByProviderTransactionId(providerTransactionId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with provider transaction ID: " + providerTransactionId));
        return mapToResponse(payment, null);
    }
    
    /**
     * Retrieves all payments for a specific order
     * 
     * @param orderId Order ID
     * @return List of payment responses
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        return payments.stream()
                .map(payment -> mapToResponse(payment, null))
                .toList();
    }
    
    /**
     * Retrieves payments by status with pagination
     * 
     * @param status Payment status
     * @param pageable Pagination parameters
     * @return Page of payment responses
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByStatus(String status, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findByStatus(status, pageable);
        return payments.map(payment -> mapToResponse(payment, null));
    }
    
    /**
     * Updates payment status and handles state transitions
     * 
     * @param transactionCode Transaction code
     * @param newStatus New payment status
     * @return Updated payment response
     */
    @Transactional
    public PaymentResponse updatePaymentStatus(String transactionCode, String newStatus) {
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with transaction code: " + transactionCode));
        
        String oldStatus = payment.getStatus();
        
        // Validate status transition
        if (oldStatus.equals(newStatus)) {
            log.warn("Payment {} already in status {}", transactionCode, newStatus);
            return mapToResponse(payment, null);
        }
        
        // Update status
        payment.setStatus(newStatus);
        
        // Update timestamps based on status
        if ("SUCCESS".equals(newStatus)) {
            payment.setPaidAt(LocalDateTime.now());
        }
        
        payment = paymentRepository.save(payment);
        
        // Publish status change events
        publishStatusChangeEvent(payment, oldStatus, newStatus);
        
        log.info("Payment status updated: {} from {} to {}", transactionCode, oldStatus, newStatus);
        
        return mapToResponse(payment, null);
    }
    
    /**
     * Handles payment webhook from PayOS
     * 
     * @param providerTransactionId Provider transaction ID
     * @param status Payment status from provider
     */
    @Transactional
    public void handlePaymentWebhook(String providerTransactionId, String status) {
        try {
            Payment payment = paymentRepository.findByProviderTransactionId(providerTransactionId)
                    .orElseThrow(() -> new PaymentNotFoundException(
                            "Payment not found with provider transaction ID: " + providerTransactionId));
            
            String newStatus = mapProviderStatus(status);
            
            if (!payment.getStatus().equals(newStatus)) {
                updatePaymentStatus(payment.getTransactionCode(), newStatus);
            }
            
        } catch (Exception e) {
            log.error("Error handling payment webhook for provider transaction ID {}: ", 
                    providerTransactionId, e);
            throw new RuntimeException("Failed to handle payment webhook: " + e.getMessage(), e);
        }
    }
    
    /**
     * Maps provider status to internal status
     * 
     * @param providerStatus Status from payment provider
     * @return Internal payment status
     */
    private String mapProviderStatus(String providerStatus) {
        return switch (providerStatus.toUpperCase()) {
            case "PAID", "SUCCESS", "COMPLETED" -> "SUCCESS";
            case "CANCELLED", "CANCELED" -> "CANCELLED";
            case "EXPIRED" -> "FAILED";
            case "FAILED", "REJECTED" -> "FAILED";
            default -> "PENDING";
        };
    }
    
    /**
     * Generates a unique transaction code
     * 
     * @return Unique transaction code
     */
    private String generateTransactionCode() {
        String code;
        do {
            code = "PAY-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
        } while (paymentRepository.findByTransactionCode(code).isPresent());
        return code;
    }
    
    /**
     * Generates order code from payment ID
     * 
     * @param paymentId Payment ID
     * @return Order code
     */
    private long generateOrderCode(Long paymentId) {
        // Generate a 7-digit order code based on payment ID and timestamp
        return (paymentId * 1000 + System.currentTimeMillis() % 1000) % 10000000;
    }
    
    /**
     * Publishes payment initiated event to Kafka
     * 
     * @param payment Payment entity
     * @param userId User ID from request
     */
    private void publishPaymentInitiatedEvent(Payment payment, String userId) {
        try {
            PaymentInitiatedEvent event = new PaymentInitiatedEvent();
            event.setPaymentId(payment.getId());
            event.setOrderId(payment.getOrder() != null ? payment.getOrder().getId() : null);
            event.setUserId(userId != null ? Long.parseLong(userId) : null);
            event.setAmount(payment.getAmount());
            event.setTimestamp(LocalDateTime.now());
            
            kafkaTemplate.send("payment-initiated", event);
            log.debug("Published payment initiated event for payment ID: {}", payment.getId());
        } catch (Exception e) {
            log.error("Failed to publish payment initiated event for payment ID: {}", 
                    payment.getId(), e);
        }
    }
    
    /**
     * Publishes status change events to Kafka
     * 
     * @param payment Payment entity
     * @param oldStatus Old status
     * @param newStatus New status
     */
    private void publishStatusChangeEvent(Payment payment, String oldStatus, String newStatus) {
        try {
            if ("SUCCESS".equals(newStatus)) {
                PaymentCompletedEvent event = new PaymentCompletedEvent();
                event.setPaymentId(payment.getId());
                event.setOrderId(payment.getOrder() != null ? payment.getOrder().getId() : null);
                event.setUserId(payment.getOrder() != null && payment.getOrder().getUserId() != null 
                        ? payment.getOrder().getUserId() : null);
                event.setAmount(payment.getAmount());
                event.setTimestamp(LocalDateTime.now());
                
                kafkaTemplate.send("payment-completed", event);
                log.debug("Published payment completed event for payment ID: {}", payment.getId());
                
            } else if ("FAILED".equals(newStatus) || "CANCELLED".equals(newStatus)) {
                PaymentFailedEvent event = new PaymentFailedEvent();
                event.setPaymentId(payment.getId());
                event.setOrderId(payment.getOrder() != null ? payment.getOrder().getId() : null);
                event.setUserId(payment.getOrder() != null && payment.getOrder().getUserId() != null 
                        ? payment.getOrder().getUserId() : null);
                event.setTimestamp(LocalDateTime.now());
                
                kafkaTemplate.send("payment-failed", event);
                log.debug("Published payment failed event for payment ID: {}", payment.getId());
            }
        } catch (Exception e) {
            log.error("Failed to publish status change event for payment ID: {}", 
                    payment.getId(), e);
        }
    }
    
    /**
     * Maps Payment entity to PaymentResponse DTO
     * 
     * @param payment Payment entity
     * @param paymentUrl Optional payment URL
     * @return Payment response DTO
     */
    private PaymentResponse mapToResponse(Payment payment, String paymentUrl) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setOrderCode(payment.getProviderTransactionId() != null 
                ? Long.parseLong(payment.getProviderTransactionId()) : null);
        response.setUserId(payment.getOrder() != null && payment.getOrder().getUserId() != null 
                ? String.valueOf(payment.getOrder().getUserId()) : null);
        response.setAmount(payment.getAmount());
        response.setDescription(null); // Not stored in Payment entity
        response.setStatus(PaymentStatus.valueOf(payment.getStatus()));
        response.setPaymentUrl(paymentUrl);
        response.setReferenceCode(payment.getTransactionCode());
        response.setTransactionId(payment.getProviderTransactionId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        response.setPaidAt(payment.getPaidAt());
        response.setCancelledAt(null); // Not stored in Payment entity
        return response;
    }
}