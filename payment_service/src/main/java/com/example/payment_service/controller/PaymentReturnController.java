package com.example.payment_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller for handling payment return and cancel callbacks from PayOS
 * These endpoints redirect users back to the frontend after payment
 */
@RestController
@Slf4j
@Tag(name = "Payment Return", description = "Handle payment return/cancel from PayOS")
public class PaymentReturnController {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @GetMapping("/return")
    @Operation(summary = "Payment return", description = "Handle successful payment return from PayOS")
    public RedirectView handlePaymentReturn(
            @Parameter(description = "Order code from PayOS")
            @RequestParam(required = false) String orderCode,
            
            @Parameter(description = "Payment status")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Payment code")
            @RequestParam(required = false) String code,
            
            @Parameter(description = "Payment ID")
            @RequestParam(required = false) String id,
            
            @Parameter(description = "Cancel flag")
            @RequestParam(required = false) String cancel
    ) {
        log.info("Payment return callback - orderCode={}, status={}, code={}, id={}, cancel={}", 
                orderCode, status, code, id, cancel);
        
        try {
            // Build redirect URL to frontend
            StringBuilder redirectUrl = new StringBuilder(frontendUrl);
            redirectUrl.append("/payment/result?");
            
            // Add query parameters
            if (orderCode != null) {
                redirectUrl.append("orderCode=").append(encode(orderCode)).append("&");
            }
            if (status != null) {
                redirectUrl.append("status=").append(encode(status)).append("&");
            }
            if (code != null) {
                redirectUrl.append("code=").append(encode(code)).append("&");
            }
            if (id != null) {
                redirectUrl.append("id=").append(encode(id)).append("&");
            }
            if (cancel != null) {
                redirectUrl.append("cancel=").append(encode(cancel)).append("&");
            }
            
            // Determine success or failure
            boolean isSuccess = "PAID".equalsIgnoreCase(status) || 
                               "00".equals(code) ||
                               (cancel == null || !"true".equalsIgnoreCase(cancel));
            
            redirectUrl.append("success=").append(isSuccess);
            
            String finalUrl = redirectUrl.toString();
            log.info("Redirecting to frontend: {}", finalUrl);
            
            return new RedirectView(finalUrl);
            
        } catch (Exception e) {
            log.error("Error processing payment return", e);
            // Redirect to error page
            return new RedirectView(frontendUrl + "/payment/result?success=false&error=" + encode(e.getMessage()));
        }
    }

    @GetMapping("/cancel")
    @Operation(summary = "Payment cancel", description = "Handle payment cancellation from PayOS")
    public RedirectView handlePaymentCancel(
            @Parameter(description = "Order code from PayOS")
            @RequestParam(required = false) String orderCode,
            
            @Parameter(description = "Cancellation reason")
            @RequestParam(required = false) String reason
    ) {
        log.info("Payment cancel callback - orderCode={}, reason={}", orderCode, reason);
        
        try {
            // Build redirect URL to frontend with cancel flag
            StringBuilder redirectUrl = new StringBuilder(frontendUrl);
            redirectUrl.append("/payment/result?");
            
            if (orderCode != null) {
                redirectUrl.append("orderCode=").append(encode(orderCode)).append("&");
            }
            if (reason != null) {
                redirectUrl.append("reason=").append(encode(reason)).append("&");
            }
            
            redirectUrl.append("cancel=true&success=false");
            
            String finalUrl = redirectUrl.toString();
            log.info("Redirecting to frontend (cancelled): {}", finalUrl);
            
            return new RedirectView(finalUrl);
            
        } catch (Exception e) {
            log.error("Error processing payment cancel", e);
            return new RedirectView(frontendUrl + "/payment/result?success=false&cancel=true");
        }
    }
    
    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            log.warn("Failed to encode value: {}", value, e);
            return value;
        }
    }
}
