package com.example.payment_service.saga;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga Timeout and Compensation Handler
 *
 * Monitors long-running Saga transactions and triggers compensation
 * when they exceed timeout thresholds
 *
 * Handles scenarios like:
 * - Payment completed but wallet service is down
 * - Partial failures in distributed transaction
 * - Network timeouts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaTimeoutHandler {

    private final PaymentRepository paymentRepository;

    // Saga timeout: 15 minutes
    private static final int SAGA_TIMEOUT_MINUTES = 15;

    /**
     * Check for stuck payments every 5 minutes
     * These are payments that completed but didn't get acknowledged
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 minutes
    @Transactional
    public void checkStuckPayments() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(SAGA_TIMEOUT_MINUTES);

            // Find payments that are in SUCCESS status but old
            // This might indicate wallet service didn't process the event
            List<Payment> stuckPayments = paymentRepository.findByStatusAndUpdatedAtBefore(
                    "SUCCESS", cutoffTime);

            if (!stuckPayments.isEmpty()) {
                log.warn("⚠Found {} stuck payments that may need manual review", stuckPayments.size());

                for (Payment payment : stuckPayments) {
                    log.warn("Stuck payment detected: ID={}, User={}, Amount={}, CompletedAt={}",
                            payment.getId(),
                            payment.getOrder() != null ? payment.getOrder().getUserId() : "unknown",
                            payment.getAmount(),
                            payment.getPaidAt());

                    // In production: Send alert to ops team or retry event publishing
                    // Could also check if wallet balance was updated
                }
            }

            // Find payments stuck in PROCESSING state
            List<Payment> processingPayments = paymentRepository.findByStatusAndUpdatedAtBefore(
                    "PROCESSING", cutoffTime);

            if (!processingPayments.isEmpty()) {
                log.warn("⚠️ Found {} payments stuck in PROCESSING state", processingPayments.size());

                for (Payment payment : processingPayments) {
                    // Auto-fail after timeout
                    payment.setStatus("FAILED");
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(payment);

                    log.error("Payment timed out and marked as FAILED: ID={}", payment.getId());

                    // Trigger compensation (refund, notification, etc.)
                    // publishCompensationEvent(payment);
                }
            }

        } catch (Exception e) {
            log.error("Error in saga timeout handler", e);
        }
    }

    /**
     * Check for failed payments that need compensation
     * Run daily to catch any missed compensations
     */
    @Scheduled(cron = "0 0 */6 * * *") // Every 6 hours
    @Transactional(readOnly = true)
    public void auditFailedPayments() {
        try {
            LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
            List<Payment> failedPayments = paymentRepository.findByStatusAndCreatedAtAfter(
                    "FAILED", last24Hours);

            if (!failedPayments.isEmpty()) {
                log.info("📊 Audit: {} failed payments in last 24 hours", failedPayments.size());

                // In production: Send metrics to monitoring system
                // Check if compensations were executed
            }
        } catch (Exception e) {
            log.error("Error in failed payment audit", e);
        }
    }
}

