package com.example.payment_service.service;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.entity.PaymentOrder;
import com.example.payment_service.event.producer.PaymentEventProducer;
import com.example.payment_service.payos.Webhook;
import com.example.payment_service.payos.WebhookData;
import com.example.payment_service.payos.PayOS;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentWebhookService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentOrderRepository orderRepository;
    private final InvoiceService invoiceService;
    private final PayOS payOS;
    private final PaymentEventProducer eventPublisher;
    
    /**
     * Handle PayOS webhook for payment status updates
     */
    public WebhookData handlePaymentWebhook(Webhook webhookBody) {
        try {
            // Verify webhook data
            WebhookData webhookData = payOS.verifyPaymentWebhookData(webhookBody);
            
            log.info("Processing webhook - code: {}, desc: {}", 
                    webhookData.getCode(), webhookData.getDesc());
            
            // Find payment by provider transaction ID or order code
            Payment payment = findPaymentFromWebhook(webhookData);
            
            if (payment == null) {
                log.warn("Payment not found for webhook data: {}", webhookData);
                return webhookData;
            }
            
            // Map webhook status to payment status
            String newStatus = mapWebhookStatus(webhookData.getCode());
            String oldStatus = payment.getStatus();
            
            // Update payment if status changed
            if (!oldStatus.equals(newStatus)) {
                payment.setStatus(newStatus);
                payment.setProviderTransactionId(String.valueOf(webhookData.getOrderCode()));
                
                if ("SUCCESS".equals(newStatus)) {
                    handlePaymentSuccess(payment, webhookData);
                } else if ("FAILED".equals(newStatus) || "CANCELLED".equals(newStatus)) {
                    handlePaymentFailure(payment, webhookData, newStatus);
                }
                
                paymentRepository.save(payment);
                
                // Publish payment status changed event
                publishPaymentStatusChangedEvent(payment, oldStatus, newStatus);
            }
            
            return webhookData;
            
        } catch (Exception e) {
            log.error("Error processing webhook: ", e);
            throw new RuntimeException("Failed to process webhook: " + e.getMessage());
        }
    }
    
    /**
     * Handle successful payment
     */
    private void handlePaymentSuccess(Payment payment, WebhookData webhookData) {
        payment.setPaidAt(LocalDateTime.now());
        
        // Update order status
        PaymentOrder order = payment.getOrder();
        if (order != null && "PENDING".equals(order.getStatus())) {
            order.setStatus("COMPLETED");
            orderRepository.save(order);
        }
        
        // Create invoice
        try {
            invoiceService.createInvoice(payment);
            log.info("Invoice created for payment: {}", payment.getTransactionCode());
        } catch (Exception e) {
            log.error("Failed to create invoice: {}", e.getMessage());
        }
        
        // Publish payment completed event
        publishPaymentCompletedEvent(payment, order);
        
        // Publish revenue recorded event
        publishRevenueRecordedEvent(payment);
    }
    
    /**
     * Handle failed or cancelled payment
     */
    private void handlePaymentFailure(Payment payment, WebhookData webhookData, String status) {
        // Update order status
        PaymentOrder order = payment.getOrder();
        if (order != null && "PENDING".equals(order.getStatus())) {
            order.setStatus(status);
            orderRepository.save(order);
        }
        
        // Publish payment failed event
        publishPaymentFailedEvent(payment, order, webhookData.getDesc());
    }
    
    /**
     * Find payment from webhook data
     */
    private Payment findPaymentFromWebhook(WebhookData webhookData) {
        // Try to find by provider transaction ID
        String orderCode = String.valueOf(webhookData.getOrderCode());
        
        // Search by provider transaction ID first
        return paymentRepository.findByProviderTransactionId(orderCode)
                .or(() -> {
                    // If not found, try to find recent pending payment for this order
                    return paymentRepository.findByStatus("PENDING").stream()
                            .filter(p -> p.getProviderTransactionId() != null && 
                                       p.getProviderTransactionId().equals(orderCode))
                            .findFirst();
                })
                .orElse(null);
    }
    
    /**
     * Map PayOS webhook code to payment status
     */
    private String mapWebhookStatus(String code) {
        return switch (code) {
            case "00" -> "SUCCESS";  // Payment successful
            case "01" -> "PENDING";  // Payment pending
            case "02" -> "PROCESSING";  // Payment processing
            case "03" -> "CANCELLED";  // Payment cancelled
            default -> "FAILED";  // Any other code is failure
        };
    }
    
    // ==================== Event Publishers ====================
    
    private void publishPaymentStatusChangedEvent(Payment payment, String oldStatus, String newStatus) {
        try {
            // Not using standardized method as this is a custom status change event
            // Could be added to PaymentEventPublisher if needed
            log.info("Payment status changed: {} -> {} for payment: {}", 
                    oldStatus, newStatus, payment.getTransactionCode());
        } catch (Exception e) {
            log.error("Failed to publish payment status changed event: {}", e.getMessage());
        }
    }
    
    private void publishPaymentCompletedEvent(Payment payment, PaymentOrder order) {
        try {
            Integer credits = order != null && order.getPackageInfo() != null 
                    ? order.getPackageInfo().getCredits() : 0;
            Integer bonusCredits = order != null && order.getPackageInfo() != null 
                    ? order.getPackageInfo().getBonusCredits() : 0;
            
            eventPublisher.publishPaymentCompleted(
                    payment.getId(),
                    order != null ? order.getId() : null,
                    order != null ? order.getUserId() : null,
                    payment.getAmount(),
                    credits.toString(),
                    bonusCredits.toString()
            );
        } catch (Exception e) {
            log.error("Failed to publish payment completed event: {}", e.getMessage());
        }
    }
    
    private void publishPaymentFailedEvent(Payment payment, PaymentOrder order, String reason) {
        try {
            eventPublisher.publishPaymentFailed(
                    payment.getId(),
                    order != null ? order.getId() : null,
                    order != null ? order.getUserId() : null,
                    reason
            );
        } catch (Exception e) {
            log.error("Failed to publish payment failed event: {}", e.getMessage());
        }
    }
    
    private void publishRevenueRecordedEvent(Payment payment) {
        try {
            
            eventPublisher.publishRevenueRecorded(
                    LocalDate.now(),
                    payment.getAmount(),
                    payment.getId()
            );
        } catch (Exception e) {
            log.error("Failed to publish revenue recorded event: {}", e.getMessage());
        }
    }
}
