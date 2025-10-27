package com.example.payment_service.event.consumer;

import com.example.payment_service.event.payload.UserRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for user service events
 * Listens to user-related events and triggers payment service actions
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserEventConsumer {
    ObjectMapper objectMapper;
    
    @KafkaListener(
            topics = "user.registered",
            groupId = "${spring.kafka.consumer.group-id:payment-service-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleUserRegistered(Object eventObject) {
        try {
            UserRegisteredEvent event = objectMapper.convertValue(eventObject, UserRegisteredEvent.class);
            
            log.info("Received user.registered event for user: {} ({})", 
                    event.getUsername(), event.getEmail());
            
            // Send welcome bonus offer
            sendWelcomeBonusOffer(event);
            
            log.info("Welcome bonus offer sent to user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to handle user.registered event", e);
            // In production, implement retry logic or dead letter queue
        }
    }
    
    /**
     * Send welcome bonus offer to newly registered user
     */
    private void sendWelcomeBonusOffer(UserRegisteredEvent event) {
        // TODO: Integrate with notification service to send welcome email
        
        log.info("Sending welcome bonus offer to user: {} ({})", 
                event.getUsername(), event.getEmail());
        
        // Welcome offer includes:
        // 1. WELCOME10: 10% off first purchase (any amount)
        // 2. NEWUSER50: 50,000 VND off on orders 200,000+
        // 3. Link to credit packages
        
        // Example welcome email content:
        String subject = "Welcome to Payment Service - Special Offers Inside!";
        String message = String.format(
                "Hi %s,\n\n" +
                "Welcome to our payment service! 🎉\n\n" +
                "As a new user, you get exclusive offers:\n" +
                "• Use code WELCOME10 for 10%% off your first purchase\n" +
                "• Use code NEWUSER50 for 50,000 VND off (minimum 200,000 VND)\n\n" +
                "Browse our credit packages and start saving today!\n\n" +
                "Best regards,\n" +
                "Payment Service Team",
                event.getUsername()
        );
        
        log.debug("Welcome email subject: {}", subject);
        log.debug("Welcome email body: {}", message);
        
        // In production:
        // notificationService.sendEmail(event.getEmail(), subject, message);
        // notificationService.sendPush(event.getUserId(), "Welcome! Check your special offers");
        
        // Optionally publish event that welcome offer was sent
        log.debug("Welcome offer event created for user: {}", event.getUserId());
    }
}
