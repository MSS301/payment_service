package com.example.payment_service.event.producer;

import com.example.payment_service.entity.OutboxEvent;
import com.example.payment_service.event.payload.*;
import com.example.payment_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Producer for publishing payment-related events to Kafka
 * NOW WITH OUTBOX PATTERN - Events are saved to DB first, then published asynchronously
 * This ensures no event loss even if service crashes before publishing to Kafka
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentEventProducer {
    KafkaTemplate<String, Object> kafkaTemplate;
    OutboxEventRepository outboxRepository;
    ObjectMapper objectMapper;

    // ============ Order Events ============
    
    public void publishOrderCreated(Long orderId, String userId, BigDecimal amount, Long packageId) {
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
    
    public void publishOrderExpired(Long orderId, String userId) {
        OrderExpiredEvent event = OrderExpiredEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send("payment.order_expired", event);
        log.info("Published payment.order_expired event for order: {}", orderId);
    }
    
    // ============ Payment Events ============
    
    public void publishPaymentInitiated(Long paymentId, Long orderId, String userId, BigDecimal amount, Integer credits) {
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
    

    /**
     * Publish Payment Completed Event using Outbox Pattern
     * This is the CRITICAL event for Saga Pattern - must not be lost
     */
    @Transactional
    public void publishPaymentCompleted(Long paymentId, Long orderId, String userId, BigDecimal amount,
                                       String currency, String paymentMethod) {
        try {
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(paymentId)
                    .orderId(orderId)
                    .userId(userId)
                    .amount(amount)
                    .currency(currency)
                    .paymentMethod(paymentMethod)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Save to outbox table atomically with payment transaction
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(paymentId.toString())
                    .aggregateType("PAYMENT")
                    .eventType("payment.completed")
                    .payload(payload)
                    .status("PENDING")
                    .build();

            outboxRepository.save(outboxEvent);

            log.info("Saved payment.completed event to outbox for payment: {} - user: {}, amount: {} {}",
                    paymentId, userId, amount, currency);
        } catch (Exception e) {
            log.error("Failed to save payment.completed event to outbox", e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }
    

    /**
     * Publish Payment Failed Event using Outbox Pattern
     * Critical for compensation in Saga Pattern
     */
    @Transactional
    public void publishPaymentFailed(Long paymentId, Long orderId, String userId, String reason) {
        try {
            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .paymentId(paymentId)
                    .orderId(orderId)
                    .userId(userId)
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(paymentId.toString())
                    .aggregateType("PAYMENT")
                    .eventType("payment.failed")
                    .payload(payload)
                    .status("PENDING")
                    .build();

            outboxRepository.save(outboxEvent);

            log.info("Saved payment.failed event to outbox for payment: {}, reason: {}", paymentId, reason);
        } catch (Exception e) {
            log.error("Failed to save payment.failed event to outbox", e);
            throw new RuntimeException("Failed to save event to outbox", e);
        }
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
    
    public void publishPromotionUsed(String userId, String promotionCode, BigDecimal discount) {
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
