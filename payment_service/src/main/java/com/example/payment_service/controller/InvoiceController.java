package com.example.payment_service.controller;

import com.example.payment_service.dto.response.InvoiceResponse;
import com.example.payment_service.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for invoice operations
 * Handles invoice retrieval and download
 */
@RestController
@RequestMapping("/api/payments/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoices", description = "Invoice management APIs")
public class InvoiceController {
    
    private final InvoiceService invoiceService;
    
    @GetMapping("/my")
    @Operation(summary = "Get my invoices", description = "Retrieve all invoices for the authenticated user")
    public ResponseEntity<List<InvoiceResponse>> getMyInvoices(
            @Parameter(description = "User ID from authentication token")
            @RequestHeader(value = "X-User-ID", required = false) String userId
    ) {
        log.info("Getting invoices for user: {}", userId);
        
        List<InvoiceResponse> invoices = invoiceService.getMyInvoices(userId);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/{invoiceNumber}")
    @Operation(summary = "Get invoice by number", description = "Retrieve invoice details by invoice number")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @Parameter(description = "Invoice number")
            @PathVariable String invoiceNumber,
            @Parameter(description = "User ID from authentication token")
            @RequestHeader(value = "X-User-ID", required = false) String userId
    ) {
        log.info("Getting invoice {} for user {}", invoiceNumber, userId);
        
        InvoiceResponse invoice = invoiceService.getInvoiceByNumber(invoiceNumber, userId);
        return ResponseEntity.ok(invoice);
    }
    
    @GetMapping("/{invoiceNumber}/download")
    @Operation(summary = "Download invoice", description = "Download invoice as text file (PDF coming soon)")
    public ResponseEntity<byte[]> downloadInvoice(
            @Parameter(description = "Invoice number")
            @PathVariable String invoiceNumber,
            @Parameter(description = "User ID from authentication token")
            @RequestHeader(value = "X-User-ID", required = false) Long userId
    ) {
        userId = userId != null ? userId : 1L; // TODO: Get from JWT auth context
        log.info("Downloading invoice {} for user {}", invoiceNumber, userId);
        
        byte[] invoiceData = invoiceService.downloadInvoice(invoiceNumber, userId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", invoiceNumber + ".txt");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(invoiceData);
    }
    
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get invoice by order", description = "Retrieve invoice for a specific order")
    public ResponseEntity<InvoiceResponse> getInvoiceByOrder(
            @Parameter(description = "Order ID")
            @PathVariable Long orderId,
            @Parameter(description = "User ID from authentication token")
            @RequestHeader(value = "X-User-ID", required = false) Long userId
    ) {
        userId = userId != null ? userId : 1L; // TODO: Get from JWT auth context
        log.info("Getting invoice for order {} by user {}", orderId, userId);
        
//        InvoiceResponse invoice = invoiceService.getMyInvoices(orderId, userId);
        return ResponseEntity.ok(null);
    }
}
