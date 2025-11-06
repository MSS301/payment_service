package com.example.payment_service.repository;

import com.example.payment_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Find pending events that are ready to be published
     * Includes failed events that are ready for retry
     */
    @Query("SELECT e FROM OutboxEvent e WHERE " +
           "(e.status = 'PENDING' OR (e.status = 'FAILED' AND e.nextRetryAt <= :now)) " +
           "AND e.retryCount < e.maxRetry " +
           "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEvents(LocalDateTime now);

    /**
     * Find old published events for cleanup
     */
    @Query("SELECT e FROM OutboxEvent e WHERE " +
           "e.status = 'PUBLISHED' AND e.publishedAt < :cutoffDate")
    List<OutboxEvent> findOldPublishedEvents(LocalDateTime cutoffDate);

    /**
     * Find events by aggregate for debugging
     */
    List<OutboxEvent> findByAggregateIdAndAggregateType(String aggregateId, String aggregateType);
}


