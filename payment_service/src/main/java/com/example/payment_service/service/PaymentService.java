package com.example.payment_service.service;

import com.example.payment_service.dto.request.CreatePaymentRequest;
import com.example.payment_service.dto.response.PaymentResponse;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.PaymentStatus;
import com.example.payment_service.event.PaymentCreatedEvent;
import com.example.payment_service.event.PaymentStatusChangedEvent;
import com.example.payment_service.exception.PaymentNotFoundException;
import com.example.payment_service.payos.*;
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
    
    /**
     * Tạo payment mới và link thanh toán PayOS
     */
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
            CheckoutResponseData checkoutResponse = payOS.createPaymentLink(paymentData);
            
            payment.setPaymentUrl(checkoutResponse.getCheckoutUrl());
            payment.setTransactionId(checkoutResponse.getPaymentLinkId());
            
            // Save payment
            payment = paymentRepository.save(payment);
            
            // Publish event
            publishPaymentCreatedEvent(payment);
            
            log.info("Payment created successfully: orderCode={}, paymentUrl={}", 
                    payment.getOrderCode(), payment.getPaymentUrl());
            
            return mapToResponse(payment);
            
        } catch (Exception e) {
            log.error("Error creating payment: ", e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage());
        }
    }
    
    /**
     * Lấy thông tin payment link từ PayOS
     */
    public PaymentResponse getPaymentLinkInfo(Long orderCode) {
        try {
            Payment payment = paymentRepository.findByOrderCode(orderCode)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + orderCode));
            
            // Get payment link information from PayOS
            PaymentLinkData paymentLinkData = payOS.getPaymentLinkInformation(orderCode);
            
            // Update payment status if changed
            String payosStatus = paymentLinkData.getStatus();
            PaymentStatus newStatus = mapPayOSStatus(payosStatus);
            
            if (!payment.getStatus().equals(newStatus)) {
                updatePaymentStatus(payment, newStatus);
            }
            
            log.info("Retrieved payment link info: orderCode={}, status={}", orderCode, payosStatus);
            
            return mapToResponse(payment);
            
        } catch (Exception e) {
            log.error("Error getting payment link info for orderCode {}: ", orderCode, e);
            throw new RuntimeException("Failed to get payment link info: " + e.getMessage());
        }
    }
    
    /**
     * Hủy payment link
     */
    public PaymentResponse cancelPayment(Long orderCode, String cancellationReason) {
        try {
            Payment payment = paymentRepository.findByOrderCode(orderCode)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + orderCode));
            
            // Cancel payment link on PayOS
            PaymentLinkData cancelledData = payOS.cancelPaymentLink(
                    orderCode, 
                    cancellationReason != null ? cancellationReason : "User cancelled"
            );
            
            // Update payment status
            updatePaymentStatus(payment, PaymentStatus.CANCELLED);
            payment.setCancelledAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
            
            log.info("Payment cancelled: orderCode={}, reason={}", orderCode, cancellationReason);
            
            return mapToResponse(payment);
            
        } catch (Exception e) {
            log.error("Error cancelling payment for orderCode {}: ", orderCode, e);
            throw new RuntimeException("Failed to cancel payment: " + e.getMessage());
        }
    }
    
    /**
     * Xác thực webhook URL
     */
    public String confirmWebhook(String webhookUrl) {
        try {
            String result = payOS.confirmWebhook(webhookUrl);
            log.info("Webhook confirmed successfully: {}", webhookUrl);
            return result;
        } catch (Exception e) {
            log.error("Error confirming webhook: ", e);
            throw new RuntimeException("Failed to confirm webhook: " + e.getMessage());
        }
    }
    
    /**
     * Xử lý webhook từ PayOS sau khi thanh toán
     */
    public WebhookData handlePaymentWebhook(Webhook webhookBody) {
        try {
            // Verify webhook data
            WebhookData webhookData = payOS.verifyPaymentWebhookData(webhookBody);
            
            Long orderCode = webhookData.getOrderCode();
            log.info("Processing webhook for orderCode: {}, code: {}", orderCode, webhookData.getCode());
            
            Payment payment = paymentRepository.findByOrderCode(orderCode)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + orderCode));
            
            // Update payment based on webhook data
            PaymentStatus newStatus = mapWebhookCode(webhookData.getCode());
            
            if (!payment.getStatus().equals(newStatus)) {
                updatePaymentStatus(payment, newStatus);
                
                if (newStatus == PaymentStatus.PAID) {
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setTransactionId(webhookData.getReference());
                }
            }
            
            payment = paymentRepository.save(payment);
            
            log.info("Webhook processed successfully: orderCode={}, newStatus={}", 
                    orderCode, newStatus);
            
            return webhookData;
            
        } catch (Exception e) {
            log.error("Error handling payment webhook: ", e);
            throw new RuntimeException("Failed to handle payment webhook: " + e.getMessage());
        }
    }
    
    /**
     * Lấy thông tin payment theo orderCode
     */
    public PaymentResponse getPaymentByOrderCode(Long orderCode) {
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + orderCode));
        return mapToResponse(payment);
    }
    
    /**
     * Lấy danh sách payments của user
     */
    public List<PaymentResponse> getUserPayments(String userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream().map(this::mapToResponse).toList();
    }
    
    /**
     * Lấy danh sách payments của user có phân trang
     */
    public Page<PaymentResponse> getUserPayments(String userId, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findByUserId(userId, pageable);
        return payments.map(this::mapToResponse);
    }
    
    /**
     * Cập nhật trạng thái payment
     */
    @Transactional
    public PaymentResponse updatePaymentStatus(Long orderCode, PaymentStatus newStatus) {
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + orderCode));
        
        updatePaymentStatus(payment, newStatus);
        payment = paymentRepository.save(payment);
        
        return mapToResponse(payment);
    }
    
    // ==================== Private Helper Methods ====================
    
    private void updatePaymentStatus(Payment payment, PaymentStatus newStatus) {
        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(newStatus);
        
        // Update timestamps based on status
        switch (newStatus) {
            case PAID -> payment.setPaidAt(LocalDateTime.now());
            case CANCELLED -> payment.setCancelledAt(LocalDateTime.now());
        }
        
        // Publish status change event
        publishPaymentStatusChangedEvent(payment, oldStatus, newStatus);
    }
    
    private void publishPaymentCreatedEvent(Payment payment) {
        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
                .paymentId(payment.getId())
                .orderCode(payment.getOrderCode())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .build();
        
        kafkaTemplate.send("payment-created", event);
    }
    
    private void publishPaymentStatusChangedEvent(Payment payment, PaymentStatus oldStatus, PaymentStatus newStatus) {
        PaymentStatusChangedEvent event = PaymentStatusChangedEvent.builder()
                .paymentId(payment.getId())
                .orderCode(payment.getOrderCode())
                .userId(payment.getUserId())
                .oldStatus(oldStatus.name())
                .newStatus(newStatus.name())
                .amount(payment.getAmount())
                .build();
        
        kafkaTemplate.send("payment-status-changed", event);
    }
    
    private Long generateOrderCode() {
        Random random = new Random();
        Long orderCode;
        do {
            orderCode = 1000000L + random.nextLong(9000000L);
        } while (paymentRepository.findByOrderCode(orderCode).isPresent());
        return orderCode;
    }
    
    private PaymentStatus mapPayOSStatus(String payosStatus) {
        return switch (payosStatus.toUpperCase()) {
            case "PAID" -> PaymentStatus.PAID;
            case "PENDING" -> PaymentStatus.PENDING;
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            case "EXPIRED" -> PaymentStatus.EXPIRED;
            default -> PaymentStatus.FAILED;
        };
    }
    
    private PaymentStatus mapWebhookCode(String code) {
        return switch (code) {
            case "00" -> PaymentStatus.PAID;
            case "01" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.FAILED;
        };
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