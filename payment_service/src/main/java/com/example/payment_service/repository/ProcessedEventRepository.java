package com.example.payment_service.repository;

import com.example.payment_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventIdAndEventType(String eventId, String eventType);

    Optional<ProcessedEvent> findByEventIdAndEventType(String eventId, String eventType);

    @Query("SELECT e FROM ProcessedEvent e WHERE e.processedAt < :cutoffDate")
    List<ProcessedEvent> findOldProcessedEvents(LocalDateTime cutoffDate);

    @Query("SELECT e FROM ProcessedEvent e WHERE " +
           "e.processingResult = 'FAILED' AND e.processedAt > :since " +
           "ORDER BY e.processedAt DESC")
    List<ProcessedEvent> findRecentFailures(LocalDateTime since);
}

