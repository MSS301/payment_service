package com.example.payment_service.service;

import com.example.payment_service.entity.ProcessedEvent;
import com.example.payment_service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Idempotent Event Processing Service for Payment Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotentEventService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional(readOnly = true)
    public boolean isEventProcessed(String eventId, String eventType) {
        boolean exists = processedEventRepository.existsByEventIdAndEventType(eventId, eventType);
        if (exists) {
            log.info("Event already processed: {} (type: {}), skipping", eventId, eventType);
        }
        return exists;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEventAsProcessed(String eventId, String eventType, String sourceService,
                                     String payload, String result, String details) {
        try {
            String payloadHash = generateHash(payload);

            ProcessedEvent processedEvent = ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .sourceService(sourceService)
                    .payloadHash(payloadHash)
                    .processingResult(result)
                    .resultDetails(details)
                    .build();

            processedEventRepository.save(processedEvent);
            log.debug("Marked event as processed: {} (type: {})", eventId, eventType);

        } catch (Exception e) {
            log.error("Failed to mark event as processed: {}", eventId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEventAsSuccess(String eventId, String eventType, String sourceService, String payload) {
        markEventAsProcessed(eventId, eventType, sourceService, payload, "SUCCESS", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEventAsFailed(String eventId, String eventType, String sourceService,
                                  String payload, String errorMessage) {
        markEventAsProcessed(eventId, eventType, sourceService, payload, "FAILED", errorMessage);
    }

    private String generateHash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating hash", e);
            return null;
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldProcessedEvents() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            List<ProcessedEvent> oldEvents = processedEventRepository.findOldProcessedEvents(cutoffDate);

            if (!oldEvents.isEmpty()) {
                processedEventRepository.deleteAll(oldEvents);
                log.info("Cleaned up {} old processed events", oldEvents.size());
            }
        } catch (Exception e) {
            log.error("Error cleaning up old processed events", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ProcessedEvent> getRecentFailures(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return processedEventRepository.findRecentFailures(since);
    }
}

