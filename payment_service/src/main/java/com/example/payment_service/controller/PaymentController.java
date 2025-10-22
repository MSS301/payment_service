package com.example.payment_service.controller;

import com.example.payment_service.dto.ApiResponse;
import com.example.payment_service.dto.request.CreatePaymentRequest;
import com.example.payment_service.dto.response.PaymentResponse;
import com.example.payment_service.entity.PaymentStatus;
import com.example.payment_service.payos.Webhook;
import com.example.payment_service.payos.WebhookData;
import com.example.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment", description = "Payment management APIs with PayOS integration")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new payment", description = "Create a new payment and get PayOS checkout link")
    public ApiResponse<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {
        
        log.info("Creating payment for user: {}", request.getUserId());
        PaymentResponse response = paymentService.createPayment(request);
        
        return ApiResponse.<PaymentResponse>builder()
                .code(1000)
                .message("Payment created successfully")
                .result(response)
                .build();
    }
    
    @GetMapping("/{orderCode}")
    @Operation(summary = "Get payment by order code", description = "Retrieve payment details by order code")
    public ApiResponse<PaymentResponse> getPayment(
            @Parameter(description = "Order code of the payment") 
            @PathVariable Long orderCode) {
        
        log.info("Getting payment with order code: {}", orderCode);
        PaymentResponse response = paymentService.getPaymentByOrderCode(orderCode);
        
        return ApiResponse.<PaymentResponse>builder()
                .code(1000)
                .message("Payment retrieved successfully")
                .result(response)
                .build();
    }
    
    @GetMapping("/{orderCode}/info")
    @Operation(summary = "Get payment link information from PayOS", 
               description = "Retrieve latest payment information from PayOS")
    public ApiResponse<PaymentResponse> getPaymentLinkInfo(
            @Parameter(description = "Order code of the payment") 
            @PathVariable Long orderCode) {
        
        log.info("Getting payment link info from PayOS for order code: {}", orderCode);
        PaymentResponse response = paymentService.getPaymentLinkInfo(orderCode);
        
        return ApiResponse.<PaymentResponse>builder()
                .code(1000)
                .message("Payment link info retrieved successfully")
                .result(response)
                .build();
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user payments", description = "Retrieve all payments for a specific user")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getUserPayments(
            @Parameter(description = "User ID") 
            @PathVariable String userId) {
        
        log.info("Getting payments for user: {}", userId);
        List<PaymentResponse> responses = paymentService.getUserPayments(userId);
        
        return ResponseEntity.ok(ApiResponse.<List<PaymentResponse>>builder()
                .code(1000)
                .message("User payments retrieved successfully")
                .result(responses)
                .build());
    }
    
    @GetMapping("/user/{userId}/paginated")
    @Operation(summary = "Get user payments with pagination", description = "Retrieve user payments with pagination")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getUserPaymentsPaginated(
            @Parameter(description = "User ID") 
            @PathVariable String userId,
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Getting paginated payments for user: {} (page: {}, size: {})", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentResponse> responses = paymentService.getUserPayments(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<PaymentResponse>>builder()
                .code(1000)
                .message("User payments retrieved successfully")
                .result(responses)
                .build());
    }
    
    @PutMapping("/{orderCode}/status")
    @Operation(summary = "Update payment status", description = "Manually update the status of a payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePaymentStatus(
            @Parameter(description = "Order code of the payment") 
            @PathVariable Long orderCode,
            @Parameter(description = "New payment status") 
            @RequestParam PaymentStatus status) {
        
        log.info("Updating payment status for order code: {} to {}", orderCode, status);
        PaymentResponse response = paymentService.updatePaymentStatus(orderCode, status);
        
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .code(1000)
                .message("Payment status updated successfully")
                .result(response)
                .build());
    }
    
    @DeleteMapping("/{orderCode}")
    @Operation(summary = "Cancel payment", description = "Cancel payment link on PayOS")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @Parameter(description = "Order code of the payment") 
            @PathVariable Long orderCode,
            @Parameter(description = "Cancellation reason") 
            @RequestParam(required = false) String reason) {
        
        log.info("Cancelling payment for order code: {}, reason: {}", orderCode, reason);
        PaymentResponse response = paymentService.cancelPayment(orderCode, reason);
        
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .code(1000)
                .message("Payment cancelled successfully")
                .result(response)
                .build());
    }
    
    @PostMapping("/webhook/confirm")
    @Operation(summary = "Confirm webhook URL", description = "Confirm webhook URL with PayOS")
    public ResponseEntity<ApiResponse<String>> confirmWebhook(
            @Parameter(description = "Webhook URL to confirm") 
            @RequestParam String webhookUrl) {
        
        log.info("Confirming webhook URL: {}", webhookUrl);
        String result = paymentService.confirmWebhook(webhookUrl);
        
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .code(1000)
                .message("Webhook confirmed successfully")
                .result(result)
                .build());
    }
    
    @PostMapping("/webhook")
    @Operation(summary = "PayOS payment webhook", description = "Handle PayOS payment webhook notifications")
    public ResponseEntity<ApiResponse<WebhookData>> handleWebhook(
            @RequestBody Webhook webhookBody) {
        
        try {
            log.info("Received PayOS webhook: {}", webhookBody);
            
            WebhookData webhookData = paymentService.handlePaymentWebhook(webhookBody);
            
            return ResponseEntity.ok(ApiResponse.<WebhookData>builder()
                    .code(1000)
                    .message("Webhook processed successfully")
                    .result(webhookData)
                    .build());
                    
        } catch (Exception e) {
            log.error("Error processing webhook: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<WebhookData>builder()
                            .code(1500)
                            .message("Error processing webhook: " + e.getMessage())
                            .build());
        }
    }
    
    @GetMapping("/return")
    @Operation(summary = "Payment return URL", description = "Handle payment return from PayOS after successful payment")
    public ResponseEntity<String> paymentReturn(
            @RequestParam Map<String, String> params) {
        
        log.info("Payment return with params: {}", params);
        
        // Extract orderCode from params
        String orderCodeStr = params.get("orderCode");
        if (orderCodeStr != null) {
            Long orderCode = Long.parseLong(orderCodeStr);
            try {
                // Get latest payment info from PayOS
                paymentService.getPaymentLinkInfo(orderCode);
            } catch (Exception e) {
                log.error("Error updating payment from return URL: ", e);
            }
        }
        
        return ResponseEntity.ok("Payment completed successfully. You can close this window.");
    }
    
    @GetMapping("/cancel")
    @Operation(summary = "Payment cancel URL", description = "Handle payment cancellation from PayOS")
    public ResponseEntity<String> paymentCancel(
            @RequestParam Map<String, String> params) {
        
        log.info("Payment cancelled with params: {}", params);
        
        // Extract orderCode from params
        String orderCodeStr = params.get("orderCode");
        if (orderCodeStr != null) {
            Long orderCode = Long.parseLong(orderCodeStr);
            try {
                // Cancel payment
                paymentService.cancelPayment(orderCode, "User cancelled from PayOS");
            } catch (Exception e) {
                log.error("Error cancelling payment from cancel URL: ", e);
            }
        }
        
        return ResponseEntity.ok("Payment cancelled. You can close this window.");
    }
}
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping
    @Operation(summary = "Create a new payment", description = "Create a new payment with PayOS")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {
        
        log.info("Creating payment for user: {}", request.getUserId());
        PaymentResponse response = paymentService.createPayment(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<PaymentResponse>builder()
                        .code(1000)
                        .message("Payment created successfully")
                        .result(response)
                        .build());
    }
    
    @GetMapping("/{orderCode}")
    @Operation(summary = "Get payment by order code", description = "Retrieve payment details by order code")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @Parameter(description = "Order code of the payment") 
            @PathVariable Long orderCode) {
        
        log.info("Getting payment with order code: {}", orderCode);
        PaymentResponse response = paymentService.getPaymentByOrderCode(orderCode);
        
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .code(1000)
                .message("Payment retrieved successfully")
                .result(response)
                .build());
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user payments", description = "Retrieve all payments for a specific user")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getUserPayments(
            @Parameter(description = "User ID") 
            @PathVariable String userId) {
        
        log.info("Getting payments for user: {}", userId);
        List<PaymentResponse> responses = paymentService.getUserPayments(userId);
        
        return ResponseEntity.ok(ApiResponse.<List<PaymentResponse>>builder()
                .code(1000)
                .message("User payments retrieved successfully")
                .result(responses)
                .build());
    }
    
    @GetMapping("/user/{userId}/paginated")
    @Operation(summary = "Get user payments with pagination", description = "Retrieve user payments with pagination")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getUserPaymentsPaginated(
            @Parameter(description = "User ID") 
            @PathVariable String userId,
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Getting paginated payments for user: {} (page: {}, size: {})", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentResponse> responses = paymentService.getUserPayments(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<PaymentResponse>>builder()
                .code(1000)
                .message("User payments retrieved successfully")
                .result(responses)
                .build());
    }
    
    @PutMapping("/{orderCode}/status")
    @Operation(summary = "Update payment status", description = "Update the status of a payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePaymentStatus(
            @Parameter(description = "Order code of the payment") 
            @PathVariable Long orderCode,
            @Parameter(description = "New payment status") 
            @RequestParam PaymentStatus status) {
        
        log.info("Updating payment status for order code: {} to {}", orderCode, status);
        PaymentResponse response = paymentService.updatePaymentStatus(orderCode, status);
        
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .code(1000)
                .message("Payment status updated successfully")
                .result(response)
                .build());
    }
    
    @PostMapping("/webhook")
    @Operation(summary = "PayOS webhook", description = "Handle PayOS payment webhook")
    public ResponseEntity<ApiResponse<String>> handleWebhook(
            @RequestBody Map<String, Object> webhookData) {
        
        try {
            log.info("Received PayOS webhook: {}", webhookData);
            
            // Extract data from webhook
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
            Long orderCode = ((Number) data.get("orderCode")).longValue();
            String status = (String) data.get("status");
            
            paymentService.handlePaymentWebhook(orderCode, status);
            
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .code(1000)
                    .message("Webhook processed successfully")
                    .result("OK")
                    .build());
                    
        } catch (Exception e) {
            log.error("Error processing webhook: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<String>builder()
                            .code(1500)
                            .message("Error processing webhook: " + e.getMessage())
                            .build());
        }
    }
    
    @GetMapping("/return")
    @Operation(summary = "Payment return", description = "Handle payment return from PayOS")
    public ResponseEntity<String> paymentReturn(
            @RequestParam Map<String, String> params) {
        
        log.info("Payment return with params: {}", params);
        // Handle return logic here
        return ResponseEntity.ok("Payment completed successfully");
    }
    
    @GetMapping("/cancel")
    @Operation(summary = "Payment cancel", description = "Handle payment cancellation from PayOS")
    public ResponseEntity<String> paymentCancel(
            @RequestParam Map<String, String> params) {
        
        log.info("Payment cancelled with params: {}", params);
        // Handle cancellation logic here
        return ResponseEntity.ok("Payment cancelled");
    }
}