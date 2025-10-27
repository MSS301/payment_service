package com.example.payment_service.service;

import com.example.payment_service.dto.response.InvoiceResponse;
import com.example.payment_service.entity.Invoice;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.repository.InvoiceRepository;
import com.example.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    
    /**
     * Create invoice for successful payment
     */
    public Invoice createInvoice(Payment payment) {
        // Check if invoice already exists
        if (invoiceRepository.findByPaymentId(payment.getId()).isPresent()) {
            log.warn("Invoice already exists for payment: {}", payment.getId());
            return invoiceRepository.findByPaymentId(payment.getId()).get();
        }
        
        String invoiceNumber = generateInvoiceNumber();
        BigDecimal taxAmount = payment.getAmount().multiply(BigDecimal.valueOf(0.10)); // 10% tax
        BigDecimal totalAmount = payment.getAmount().add(taxAmount);
        
        Invoice invoice = Invoice.builder()
                .payment(payment)
                .invoiceNumber(invoiceNumber)
                .userId(payment.getOrder().getUserId())
                .amount(payment.getAmount())
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .currency(payment.getCurrency())
                .description("Payment for order " + payment.getOrder().getOrderCode())
                .build();
        
        invoice = invoiceRepository.save(invoice);
        
        log.info("Invoice created: invoiceNumber={}, paymentId={}", invoiceNumber, payment.getId());
        
        return invoice;
    }
    
    /**
     * Get user's invoices
     */
    public List<InvoiceResponse> getMyInvoices(Long userId) {
        List<Invoice> invoices = invoiceRepository.findByUserId(userId);
        return invoices.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get invoice by number
     */
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber, Long userId) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        if (!invoice.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to invoice");
        }
        
        return mapToResponse(invoice);
    }
    
    /**
     * Generate invoice PDF (placeholder)
     */
    public byte[] downloadInvoice(String invoiceNumber, Long userId) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        if (!invoice.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to invoice");
        }
        
        // TODO: Implement PDF generation
        String invoiceContent = generateInvoiceText(invoice);
        return invoiceContent.getBytes();
    }
    
    // ==================== Helper Methods ====================
    
    private String generateInvoiceNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "INV-" + timestamp;
    }
    
    private String generateInvoiceText(Invoice invoice) {
        return String.format("""
                ================================
                         INVOICE
                ================================
                Invoice Number: %s
                Date: %s
                
                Customer ID: %s
                Transaction: %s
                
                --------------------------------
                Amount:          %,.2f %s
                Tax (10%%):       %,.2f %s
                --------------------------------
                Total:           %,.2f %s
                ================================
                
                Description: %s
                
                Thank you for your business!
                """,
                invoice.getInvoiceNumber(),
                invoice.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                invoice.getUserId(),
                invoice.getPayment().getTransactionCode(),
                invoice.getAmount(),
                invoice.getCurrency(),
                invoice.getTaxAmount(),
                invoice.getCurrency(),
                invoice.getTotalAmount(),
                invoice.getCurrency(),
                invoice.getDescription()
        );
    }
    
    private InvoiceResponse mapToResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .userId(invoice.getUserId())
                .paymentId(invoice.getPayment().getId())
                .transactionCode(invoice.getPayment().getTransactionCode())
                .amount(invoice.getAmount())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .description(invoice.getDescription())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
