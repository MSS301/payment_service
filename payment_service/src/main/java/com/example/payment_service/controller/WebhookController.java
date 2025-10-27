package com.example.payment_service.controller;

import com.example.payment_service.payos.Webhook;
import com.example.payment_service.service.PaymentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for payment gateway webhooks
 * Handles webhook callbacks from PayOS and other payment providers
 */
@RestController
@RequestMapping("/webhooks/payment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Payment webhook handling APIs")
public class WebhookController {
    
    private final PaymentWebhookService webhookService;
    
    @PostMapping("/payos")
    @Operation(summary = "PayOS webhook", description = "Handle webhook callback from PayOS payment gateway")
    public ResponseEntity<Map<String, Object>> handlePayOSWebhook(
            @RequestBody Webhook webhook
    ) {
        log.info("Received PayOS webhook: orderCode={}, code={}", 
                webhook.getData().getOrderCode(), 
                webhook.getCode());
        
        try {
            webhookService.handlePaymentWebhook(webhook);
            
            return ResponseEntity.ok(Map.of(
                    "error", 0,
                    "message", "Webhook processed successfully",
                    "data", webhook.getData()
            ));
        } catch (Exception e) {
            log.error("Error processing PayOS webhook", e);
            
            return ResponseEntity.ok(Map.of(
                    "error", -1,
                    "message", "Webhook processing failed: " + e.getMessage(),
                    "data", webhook.getData()
            ));
        }
    }
    
    @PostMapping("/momo")
    @Operation(summary = "MoMo webhook", description = "Handle webhook callback from MoMo e-wallet")
    public ResponseEntity<Map<String, Object>> handleMoMoWebhook(
            @RequestBody Map<String, Object> payload
    ) {
        log.info("Received MoMo webhook: {}", payload);
        
        // TODO: Implement MoMo webhook handling
        return ResponseEntity.ok(Map.of(
                "resultCode", 0,
                "message", "Webhook received (not yet implemented)"
        ));
    }
    
    @PostMapping("/vnpay")
    @Operation(summary = "VNPay webhook", description = "Handle webhook callback from VNPay gateway")
    public ResponseEntity<Map<String, Object>> handleVNPayWebhook(
            @RequestBody Map<String, Object> payload
    ) {
        log.info("Received VNPay webhook: {}", payload);
        
        // TODO: Implement VNPay webhook handling
        return ResponseEntity.ok(Map.of(
                "RspCode", "00",
                "Message", "Webhook received (not yet implemented)"
        ));
    }
    
    @GetMapping("/payos/status")
    @Operation(summary = "PayOS webhook status", description = "Check PayOS webhook configuration status")
    public ResponseEntity<Map<String, Object>> getWebhookStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "provider", "PayOS",
                "endpoint", "/webhooks/payment/payos",
                "message", "Webhook endpoint is ready to receive PayOS callbacks"
        ));
    }
}
