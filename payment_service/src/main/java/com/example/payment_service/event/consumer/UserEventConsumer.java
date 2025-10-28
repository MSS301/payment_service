package com.example.payment_service.event.consumer;

import com.example.payment_service.event.payload.UserRegisteredEvent;
import com.example.payment_service.service.EmailService;
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
 * Follows auth_service pattern with EmailService integration
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserEventConsumer {
    ObjectMapper objectMapper;
    EmailService emailService;
    
    @KafkaListener(
            topics = "user.registered",
            groupId = "${spring.kafka.consumer.group-id:payment-service-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleUserRegistered(String eventJson) {
        try {
            // Deserialize JSON string to UserRegisteredEvent
            UserRegisteredEvent event = objectMapper.readValue(eventJson, UserRegisteredEvent.class);
            
            log.info("Received user.registered event for user: {} ({})", 
                    event.getUsername(), event.getEmail());
            
            // Send welcome bonus offer via email
            sendWelcomeBonusOffer(event);
            
            log.info("Welcome bonus offer sent to user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to handle user.registered event", e);
            // In production, implement retry logic or dead letter queue
        }
    }
    
    /**
     * Send welcome bonus offer to newly registered user
     * Uses EmailService following auth_service pattern
     */
    private void sendWelcomeBonusOffer(UserRegisteredEvent event) {
        log.info("Sending welcome bonus offer to user: {} ({})", 
                event.getUsername(), event.getEmail());
        
        // Send email using EmailService (follows auth_service pattern)
        emailService.sendWelcomeBonusEmail(event.getEmail(), event.getUsername());
        
        log.debug("Welcome offer email queued for user: {}", event.getUserId());
    }
}
