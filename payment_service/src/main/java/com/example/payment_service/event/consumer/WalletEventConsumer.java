package com.example.payment_service.event.consumer;

import com.example.payment_service.event.payload.BalanceLowEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for wallet service events
 * Listens to wallet-related events and triggers payment service actions
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WalletEventConsumer {
    ObjectMapper objectMapper;
    
    @KafkaListener(
            topics = "wallet.balance_low",
            groupId = "${spring.kafka.consumer.group-id:payment-service-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleBalanceLow(Object eventObject) {
        try {
            BalanceLowEvent event = objectMapper.convertValue(eventObject, BalanceLowEvent.class);
            
            log.info("Received wallet.balance_low event for user: {} - balance: {}", 
                    event.getUserId(), event.getBalance());
            
            // Send promotion notification to encourage top-up
            sendPromotionNotification(event);
            
            log.info("Promotion notification sent to user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to handle wallet.balance_low event", e);
            // In production, implement retry logic or dead letter queue
        }
    }
    
    /**
     * Send promotion notification to user with low balance
     */
    private void sendPromotionNotification(BalanceLowEvent event) {
        // TODO: Integrate with notification service to send email/push notification
        
        log.info("Sending promotion notification to user: {} (balance: {})", 
                event.getUserId(), event.getBalance());
        
        // Recommended promotions based on balance:
        // - WELCOME10: 10% off for first-time buyers
        // - FLASH25: 25% off for urgent top-ups
        // - VIP20: 20% off for high-value packages
        
        // Example notification message:
        String message = String.format(
                "Your balance is low (%s credits). Top up now and get special discounts! " +
                "Use codes: WELCOME10 (10%% off) or FLASH25 (25%% off)",
                event.getBalance()
        );
        
        log.debug("Notification message: {}", message);
        
        // In production:
        // notificationService.sendEmail(event.getUserId(), "Low Balance - Special Offers", message);
        // notificationService.sendPush(event.getUserId(), "Special top-up offers available!");
    }
}
