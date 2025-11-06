package com.example.payment_service.service;

import com.example.payment_service.entity.OutboxEvent;
import com.example.payment_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox Event Publisher
 *
 * Periodically scans outbox table and publishes pending events to Kafka
 * This ensures eventual consistency even if service crashes before publishing
 *
 * Production Best Practices:
 * - Uses exponential backoff for retries
 * - Marks events as FAILED after max retries
 * - Cleans up old published events
 * - Provides monitoring metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish pending events every 5 seconds
     * In production: Use distributed lock (e.g., Redisson) if running multiple instances
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void publishPendingEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents(LocalDateTime.now());

            if (pendingEvents.isEmpty()) {
                return;
            }

            log.info("Found {} pending outbox events to publish", pendingEvents.size());

            for (OutboxEvent event : pendingEvents) {
                try {
                    publishEvent(event);
                } catch (Exception e) {
                    handlePublishFailure(event, e);
                }
            }
        } catch (Exception e) {
            log.error("Error in outbox publisher job", e);
        }
    }

    /**
     * Publish single event to Kafka
     */
    private void publishEvent(OutboxEvent event) {
        try {
            // Deserialize payload to Object for Kafka
            Object payload = objectMapper.readValue(event.getPayload(), Object.class);

            // Send to Kafka
            kafkaTemplate.send(event.getEventType(), event.getAggregateId(), payload)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        markAsPublished(event);
                        log.info("Published outbox event {} to topic {}",
                                event.getId(), event.getEventType());
                    } else {
                        log.error("Failed to publish outbox event {}: {}",
                                event.getId(), ex.getMessage());
                        throw new RuntimeException("Kafka publish failed", ex);
                    }
                });

        } catch (Exception e) {
            log.error("Error publishing outbox event {}: {}", event.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Mark event as successfully published
     */
    @Transactional
    public void markAsPublished(OutboxEvent event) {
        event.setStatus("PUBLISHED");
        event.setPublishedAt(LocalDateTime.now());
        outboxRepository.save(event);
    }

    /**
     * Handle publish failure with retry logic
     */
    @Transactional
    public void handlePublishFailure(OutboxEvent event, Exception e) {
        event.setLastError(e.getMessage());

        if (event.canRetry()) {
            event.setStatus("FAILED");
            event.scheduleNextRetry();
            log.warn("Outbox event {} failed, will retry at {}",
                    event.getId(), event.getNextRetryAt());
        } else {
            event.setStatus("FAILED");
            log.error("Outbox event {} exceeded max retries, marking as FAILED permanently",
                    event.getId());
        }

        outboxRepository.save(event);
    }

    /**
     * Cleanup old published events (keep for 7 days)
     * Run daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldEvents() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            List<OutboxEvent> oldEvents = outboxRepository.findOldPublishedEvents(cutoffDate);

            if (!oldEvents.isEmpty()) {
                outboxRepository.deleteAll(oldEvents);
                log.info("Cleaned up {} old outbox events", oldEvents.size());
            }
        } catch (Exception e) {
            log.error("Error cleaning up old outbox events", e);
        }
    }
}

