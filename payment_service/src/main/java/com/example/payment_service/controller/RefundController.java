package com.example.payment_service.controller;

import com.example.payment_service.dto.request.RefundRequest;
import com.example.payment_service.dto.response.RefundResponse;
import com.example.payment_service.entity.PaymentRefund;
import com.example.payment_service.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for refund operations (Admin only)
 * Handles refund creation and management
 */
@RestController
@RequestMapping("/api/payments/admin/refunds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Refunds (Admin)", description = "Refund management APIs for administrators")
public class RefundController {
    
    private final RefundService refundService;
    
    @PostMapping("/payment/{paymentId}")
    @Operation(summary = "Create refund", description = "Create a refund for a specific payment (Admin only)")
    public ResponseEntity<RefundResponse> createRefund(
            @Parameter(description = "Payment ID to refund")
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundRequest request,
            @Parameter(description = "Admin ID from authentication token")
            @RequestHeader(value = "X-Admin-ID", required = false) String adminId
    ) {
        log.info("Admin {} creating refund for payment {}", adminId, paymentId);
        
        // TODO: Get transaction code from payment ID
        // For now, we'll need to add a method in RefundService to handle this
        String transactionCode = "TXN-" + paymentId; // Placeholder
        
        PaymentRefund refund = refundService.createRefund(transactionCode, request.getReason(), adminId);
        
        RefundResponse response = RefundResponse.builder()
                .id(refund.getId())
                .paymentId(paymentId)
                .refundCode(refund.getRefundCode())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .initiatedBy(refund.getInitiatedBy())
                .createdAt(refund.getCreatedAt())
                .processedAt(refund.getProcessedAt())
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all refunds", description = "Retrieve all refunds with optional status filter (Admin only)")
    public ResponseEntity<List<RefundResponse>> getRefunds(
            @Parameter(description = "Filter by refund status (PENDING, APPROVED, REJECTED, COMPLETED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Admin ID from authentication token")
            @RequestHeader(value = "X-Admin-ID", required = false) Long adminId
    ) {
        adminId = adminId != null ? adminId : 1L; // TODO: Get from JWT auth context + verify admin role
        log.info("Admin {} retrieving refunds with status: {}", adminId, status);
        
        List<PaymentRefund> refunds = status != null 
                ? refundService.getRefundsByStatus(status) 
                : refundService.getAllRefunds();
        
        List<RefundResponse> responses = refunds.stream()
                .map(r -> RefundResponse.builder()
                        .id(r.getId())
                        .paymentId(r.getPayment().getId())
                        .refundCode(r.getRefundCode())
                        .providerRefundId(r.getProviderRefundId())
                        .amount(r.getAmount())
                        .reason(r.getReason())
                        .status(r.getStatus())
                        .initiatedBy(r.getInitiatedBy())
                        .processedAt(r.getProcessedAt())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{refundId}")
    @Operation(summary = "Get refund by ID", description = "Retrieve refund details by ID (Admin only)")
    public ResponseEntity<RefundResponse> getRefund(
            @Parameter(description = "Refund ID")
            @PathVariable Long refundId,
            @Parameter(description = "Admin ID from authentication token")
            @RequestHeader(value = "X-Admin-ID", required = false) Long adminId
    ) {
        adminId = adminId != null ? adminId : 1L; // TODO: Get from JWT auth context + verify admin role
        log.info("Admin {} retrieving refund {}", adminId, refundId);
        
        PaymentRefund refund = refundService.getRefundById(refundId);
        
        RefundResponse response = RefundResponse.builder()
                .id(refund.getId())
                .paymentId(refund.getPayment().getId())
                .refundCode(refund.getRefundCode())
                .providerRefundId(refund.getProviderRefundId())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .initiatedBy(refund.getInitiatedBy())
                .processedAt(refund.getProcessedAt())
                .createdAt(refund.getCreatedAt())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{refundId}/approve")
    @Operation(summary = "Approve refund", description = "Approve a pending refund (Admin only)")
    public ResponseEntity<RefundResponse> approveRefund(
            @Parameter(description = "Refund ID to approve")
            @PathVariable Long refundId,
            @Parameter(description = "Admin ID from authentication token")
            @RequestHeader(value = "X-Admin-ID", required = false) Long adminId
    ) {
        adminId = adminId != null ? adminId : 1L; // TODO: Get from JWT auth context + verify admin role
        log.info("Admin {} approving refund {}", adminId, refundId);
        
        PaymentRefund refund = null;
        
        RefundResponse response = RefundResponse.builder()
                .id(refund.getId())
                .paymentId(refund.getPayment().getId())
                .refundCode(refund.getRefundCode())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .initiatedBy(refund.getInitiatedBy())
                .processedAt(refund.getProcessedAt())
                .createdAt(refund.getCreatedAt())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{refundId}/reject")
    @Operation(summary = "Reject refund", description = "Reject a pending refund (Admin only)")
    public ResponseEntity<RefundResponse> rejectRefund(
            @Parameter(description = "Refund ID to reject")
            @PathVariable Long refundId,
            @Parameter(description = "Rejection reason")
            @RequestParam String reason,
            @Parameter(description = "Admin ID from authentication token")
            @RequestHeader(value = "X-Admin-ID", required = false) Long adminId
    ) {
        adminId = adminId != null ? adminId : 1L; // TODO: Get from JWT auth context + verify admin role
        log.info("Admin {} rejecting refund {} with reason: {}", adminId, refundId, reason);
        
        PaymentRefund refund = null;
        
        RefundResponse response = RefundResponse.builder()
                .id(refund.getId())
                .paymentId(refund.getPayment().getId())
                .refundCode(refund.getRefundCode())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .initiatedBy(refund.getInitiatedBy())
                .processedAt(refund.getProcessedAt())
                .createdAt(refund.getCreatedAt())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/payment/{paymentId}")
    @Operation(summary = "Get refunds for payment", description = "Retrieve all refunds for a specific payment (Admin only)")
    public ResponseEntity<List<RefundResponse>> getRefundsByPayment(
            @Parameter(description = "Payment ID")
            @PathVariable Long paymentId,
            @Parameter(description = "Admin ID from authentication token")
            @RequestHeader(value = "X-Admin-ID", required = false) Long adminId
    ) {
        adminId = adminId != null ? adminId : 1L; // TODO: Get from JWT auth context + verify admin role
        log.info("Admin {} retrieving refunds for payment {}", adminId, paymentId);
        
        List<PaymentRefund> refunds = null;
        
        List<RefundResponse> responses = refunds.stream()
                .map(r -> RefundResponse.builder()
                        .id(r.getId())
                        .paymentId(r.getPayment().getId())
                        .refundCode(r.getRefundCode())
                        .providerRefundId(r.getProviderRefundId())
                        .amount(r.getAmount())
                        .reason(r.getReason())
                        .status(r.getStatus())
                        .initiatedBy(r.getInitiatedBy())
                        .processedAt(r.getProcessedAt())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
}
