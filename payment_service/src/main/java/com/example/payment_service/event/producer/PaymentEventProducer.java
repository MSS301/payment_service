package com.example.payment_service.event.producer;

import com.example.payment_service.event.payload.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Producer for publishing payment-related events to Kafka
 * Follows wallet_service event pattern with dedicated event payload classes
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentEventProducer {
    KafkaTemplate<String, Object> kafkaTemplate;
    
    // ============ Order Events ============
    
    public void publishOrderCreated(Long orderId, Long userId, BigDecimal amount, Long packageId) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .packageId(packageId)
                .timestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send("payment.order_created", event);
        log.info("Published payment.order_created event for order: {}", orderId);
    }
    
    public void publishOrderExpired(Long orderId, Long userId) {
        OrderExpiredEvent event = OrderExpiredEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send("payment.order_expired", event);
        log.info("Published payment.order_expired event for order: {}", orderId);
    }
    
    // ============ Payment Events ============
    
    public void publishPaymentInitiated(Long paymentId, Long orderId, Long userId, BigDecimal amount, Integer credits) {
        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .credits(credits)
                .timestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send("payment.initiated", event);
        log.info("Published payment.initiated event for payment: {}", paymentId);
    }
    
    public void publishPaymentProcessing(Long paymentId, String provider) {
        PaymentProcessingEvent event = PaymentProcessingEvent.builder()
                .paymentId(paymentId)
                .provider(provider)
                .timestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send("payment.processing", event);
        log.info("Published payment.processing event for payment: {}", paymentId);
    }
    
    public void publishPaymentCompleted(Long paymentId,Long orderId, Long userId, BigDecimal amount,
                                       String currency, String paymentMethod) {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .paymentMethod(paymentMethod)
                .timestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send("payment.completed", event);
        log.info("Published payment.completed event for payment: {} - user: {}, amount: {} {}", 
                paymentId, userId, amount, currency);
    }
    
    public void publishPaymentFailed(Long paymentId, Long orderId, Long userId, String reason) {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send("payment.failed", event);
        log.info("Published payment.failed event for payment: {}, reason: {}", paymentId, reason);
    }
    
    public void publishPaymentRefunded(Long paymentId, Long refundId, BigDecimal amount, String reason) {
        PaymentRefundedEvent event = PaymentRefundedEvent.builder()
                .paymentId(paymentId)
                .refundId(refundId)
                .amount(amount)
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send("payment.refunded", event);
        log.info("Published payment.refunded event for payment: {}, refund: {}", paymentId, refundId);
    }
    
    // ============ Bonus & Promotion Events ============
    
    public void publishBonusGranted(String userId, BigDecimal amount, String reason, String referenceId) {
        BonusGrantedEvent event = BonusGrantedEvent.builder()
                .userId(userId)
                .amount(amount)
                .reason(reason)
                .referenceId(referenceId)
                .timestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send("payment.bonus_granted", event);
        log.info("Published payment.bonus_granted event for user: {} (amount: {})", userId, amount);
    }
    
    public void publishPromotionUsed(Long userId, String promotionCode, BigDecimal discount) {
        PromotionUsedEvent event = PromotionUsedEvent.builder()
                .userId(userId)
                .promotionCode(promotionCode)
                .discount(discount)
                .timestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send("payment.promotion_used", event);
        log.info("Published payment.promotion_used event for user: {}, code: {}", userId, promotionCode);
    }
    
    // ============ Analytics Events ============
    
    public void publishRevenueRecorded(LocalDate date, BigDecimal amount, Long paymentId) {
        RevenueRecordedEvent event = RevenueRecordedEvent.builder()
                .date(date)
                .amount(amount)
                .paymentId(paymentId)
                .timestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send("payment.revenue_recorded", event);
        log.debug("Published payment.revenue_recorded event: {} VND via {}", amount);
    }
}
