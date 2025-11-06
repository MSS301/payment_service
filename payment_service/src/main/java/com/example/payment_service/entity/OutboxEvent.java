package com.example.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Outbox Pattern Implementation
 * Ensures event publishing is atomic with business transaction
 * Prevents event loss when service crashes before publishing to Kafka
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status,created_at"),
        @Index(name = "idx_outbox_event_type", columnList = "event_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId; // e.g., payment ID

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType; // e.g., "PAYMENT"

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // e.g., "payment.completed"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload; // JSON serialized event

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING / PUBLISHED / FAILED

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retry", nullable = false)
    @Builder.Default
    private Integer maxRetry = 5;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * Calculate exponential backoff for retry
     */
    public void scheduleNextRetry() {
        this.retryCount++;
        // Exponential backoff: 2^retryCount seconds
        long secondsDelay = (long) Math.pow(2, this.retryCount);
        this.nextRetryAt = LocalDateTime.now().plusSeconds(secondsDelay);
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetry;
    }
}