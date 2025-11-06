package com.example.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Idempotency Key tracking for event processing in Payment Service
 * Prevents duplicate event processing
 */
@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_event_id_type", columnList = "event_id,event_type", unique = true),
    @Index(name = "idx_processed_at", columnList = "processed_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "source_service", length = 50)
    private String sourceService;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "processing_result", length = 20)
    private String processingResult; // SUCCESS / FAILED / SKIPPED

    @Column(name = "result_details", columnDefinition = "TEXT")
    private String resultDetails;
}

