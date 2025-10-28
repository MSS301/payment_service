package com.example.payment_service.event.consumer;

import com.example.payment_service.event.payload.BalanceLowEvent;
import com.example.payment_service.service.EmailService;
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
 * Follows auth_service pattern with EmailService integration
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WalletEventConsumer {
    ObjectMapper objectMapper;
    EmailService emailService;
    
    @KafkaListener(
            topics = "wallet.balance_low",
            groupId = "${spring.kafka.consumer.group-id:payment-service-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleBalanceLow(String eventJson) {
        try {
            // Deserialize JSON string to BalanceLowEvent
            BalanceLowEvent event = objectMapper.readValue(eventJson, BalanceLowEvent.class);
            
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
     * Uses EmailService following auth_service pattern
     */
    private void sendPromotionNotification(BalanceLowEvent event) {
        log.info("Sending promotion notification to user: {} (balance: {})", 
                event.getUserId(), event.getBalance());
        
        // Send email using EmailService (follows auth_service pattern)
        // Note: In production, you'd need to fetch user's email and username from user service
        // For now, we'll use userId as username and construct a demo email
        String userEmail = event.getUserId() + "@example.com"; // TODO: Get real email from user service
        String username = event.getUserId(); // TODO: Get real username from user service
        
        emailService.sendLowBalanceEmail(userEmail, username, event.getBalance().toString());
        
        log.debug("Low balance notification email queued for user: {}", event.getUserId());
    }
}
