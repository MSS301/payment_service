package com.example.payment_service.service;

import com.example.payment_service.dto.request.CreateOrderRequest;
import com.example.payment_service.dto.response.OrderResponse;
import com.example.payment_service.entity.*;
import com.example.payment_service.entity.Package;
import com.example.payment_service.event.producer.PaymentEventProducer;
import com.example.payment_service.payos.*;
import com.example.payment_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {
    
    private final PaymentOrderRepository orderRepository;
    private final PackageRepository packageRepository;
    private final PaymentRepository paymentRepository;
    private final PromotionService promotionService;
    private final PayOS payOS;
    private final PaymentEventProducer eventPublisher;
    
    @Value("${payos.enabled:true}")
    private boolean payosEnabled;

    @Value("${payos.return-url:http://localhost:8084/payment/return}")
    private String returnUrl;
    
    @Value("${payos.cancel-url:http://localhost:8084/payment/cancel}")
    private String cancelUrl;
    
    /**
     * Create new payment order with package and optional promotion
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        try {
            // 1. Get package
            Package pkg = packageRepository.findById(request.getPackageId())
                    .orElseThrow(() -> new RuntimeException("Package not found"));
            
            if (!pkg.getIsActive()) {
                throw new RuntimeException("Package is not active");
            }
            
            // 3. Calculate amounts
            BigDecimal totalAmount = pkg.getPrice();
            BigDecimal discountAmount = BigDecimal.ZERO;
            
            // Apply promotion if provided
            if (request.getPromotionCode() != null && !request.getPromotionCode().isEmpty()) {
                try {
                    discountAmount = promotionService.applyPromotion(
                            request.getPromotionCode(), 
                            request.getUserId(), 
                            totalAmount
                    );
                } catch (Exception e) {
                    log.warn("Failed to apply promotion: {}", e.getMessage());
                    // Continue without promotion
                }
            }
            
            BigDecimal finalAmount = totalAmount.subtract(discountAmount);
            
            // 4. Create order
            String orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            
            PaymentOrder order = PaymentOrder.builder()
                    .userId(request.getUserId())
                    .packageInfo(pkg)
                    .orderCode(orderCode)
                    .totalAmount(totalAmount)
                    .discountAmount(discountAmount)
                    .finalAmount(finalAmount)
                    .currency("VND")
                    .status("PENDING")
                    .description("Purchase " + pkg.getName())
                    .build();
            
            order = orderRepository.save(order);
            
            // 5. Create payment
            String transactionCode = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            Long payOSOrderCode = System.currentTimeMillis(); // For PayOS compatibility
            
            Payment payment = Payment.builder()
                    .order(order)
                    .transactionCode(transactionCode)
                    .amount(finalAmount)
                    .currency("VND")
                    .status("PENDING")
                    .expiredAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            
            // 6. Create PayOS payment link if enabled
            String paymentUrl = null;
            if (payosEnabled) {
                try {
                    ItemData item = ItemData.builder()
                            .name(pkg.getName() + " - " + pkg.getCredits() + " credits")
                            .quantity(1)
                            .price(finalAmount.intValue())
                            .build();

                    // PayOS requires description to be max 25 characters
                    String payosDescription = order.getDescription();
                    if (payosDescription.length() > 25) {
                        payosDescription = payosDescription.substring(0, 25);
                    }

                    PaymentData paymentData = PaymentData.builder()
                            .orderCode(payOSOrderCode)
                            .amount(finalAmount.intValue())
                            .description(payosDescription)
                            .items(List.of(item))
                            .returnUrl(returnUrl)
                            .cancelUrl(cancelUrl)
                            .build();

                    CheckoutResponseData checkoutResponse = payOS.createPaymentLink(paymentData);
                    paymentUrl = checkoutResponse.getCheckoutUrl();
                    payment.setProviderTransactionId(checkoutResponse.getPaymentLinkId());

                    log.info("PayOS payment link created successfully: orderCode={}, paymentLinkId={}",
                            orderCode, checkoutResponse.getPaymentLinkId());
                } catch (Exception e) {
                    log.error("Failed to create PayOS payment link, order will be created without payment URL: {}",
                            e.getMessage());
                    // Order and payment record will still be saved, but without PayOS payment link
                    // User can retry payment later or use alternative payment method
                }
            } else {
                log.info("PayOS integration is disabled. Order created without payment link: orderCode={}", orderCode);
            }

            payment = paymentRepository.save(payment);
            
            // 7. Record promotion usage if applied (commented out to prevent transaction rollback)
            // TODO: Move to @TransactionalEventListener after commit
            if (discountAmount.compareTo(BigDecimal.ZERO) > 0 && request.getPromotionCode() != null) {
                promotionService.recordPromotionUsage(
                        request.getPromotionCode(),
                        request.getUserId(),
                        order,
                        discountAmount
                );

//                publishPromotionUsedEvent(request.getUserId(), request.getPromotionCode(), discountAmount);
            }

            // 8-9. Event publishing commented out to prevent transaction rollback
            // TODO: Use @TransactionalEventListener pattern for non-critical events
//            publishOrderCreatedEvent(order, pkg);
//            publishPaymentInitiatedEvent(payment, order, pkg);

            log.info("Order created: orderCode={}, amount={}, paymentUrl={}", 
                    orderCode, finalAmount, paymentUrl);
            
            return mapToOrderResponse(order, pkg, paymentUrl);
            
        } catch (Exception e) {
            log.error("Error creating order: ", e);
            throw new RuntimeException("Failed to create order: " + e.getMessage());
        }
    }
    
    /**
     * Get order by ID
     */
    public OrderResponse getOrder(Long orderId, Long userId) {
        PaymentOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Check ownership
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to order");
        }
        
        Package pkg = order.getPackageInfo();
        
        // Get payment URL if exists
        String paymentUrl = null;
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            if ("PENDING".equals(payment.getStatus()) && payment.getProviderTransactionId() != null) {
                try {
                    // Try to parse as Long only if it's a numeric value
                    Long paymentLinkId = null;
                    String providerTxnId = payment.getProviderTransactionId();

                    // Check if it's a numeric value (PayOS payment link ID)
                    if (providerTxnId.matches("\\d+")) {
                        paymentLinkId = Long.parseLong(providerTxnId);
                    } else {
                        // If it's not numeric, skip getting payment URL
                        // This can happen if webhook already updated with order code string
                        log.debug("Provider transaction ID is not numeric: {}", providerTxnId);
                    }

                    if (paymentLinkId != null) {
                        PaymentLinkData linkData = payOS.getPaymentLinkInformation(paymentLinkId);
                        paymentUrl = linkData.getCheckoutUrl();
                    }
                } catch (Exception e) {
                    log.warn("Failed to get payment URL: {}", e.getMessage());
                }
            }
        }
        
        return mapToOrderResponse(order, pkg, paymentUrl);
    }
    
    /**
     * Get user's orders with optional status filter
     */
    public Page<OrderResponse> getMyOrders(String userId, String status, Pageable pageable) {
        Page<PaymentOrder> orders;
        
        if (status != null && !status.isEmpty()) {
            orders = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            orders = orderRepository.findByUserId(userId, pageable);
        }
        
        return orders.map(order -> {
            Package pkg = order.getPackageInfo();
            return mapToOrderResponse(order, pkg, null);
        });
    }
    
    /**
     * Cancel order
     */
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        PaymentOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to order");
        }
        
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Can only cancel pending orders");
        }
        
        order.setStatus("CANCELLED");
        order = orderRepository.save(order);
        
        // Cancel associated payments
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        for (Payment payment : payments) {
            if ("PENDING".equals(payment.getStatus())) {
                payment.setStatus("CANCELLED");
                paymentRepository.save(payment);
                
                // Cancel PayOS payment link if exists
                if (payment.getProviderTransactionId() != null) {
                    try {
                        // Only try to cancel if provider transaction ID is numeric (PayOS payment link ID)
                        String providerTxnId = payment.getProviderTransactionId();
                        if (providerTxnId.matches("\\d+")) {
                            payOS.cancelPaymentLink(
                                    Long.parseLong(providerTxnId),
                                    "User cancelled order"
                            );
                        } else {
                            log.debug("Skipping PayOS cancel for non-numeric provider transaction ID: {}", providerTxnId);
                        }
                    } catch (Exception e) {
                        log.error("Failed to cancel PayOS payment: {}", e.getMessage());
                    }
                }
            }
        }
        
        // Publish order cancelled event
        publishOrderExpiredEvent(order);
        
        return mapToOrderResponse(order, order.getPackageInfo(), null);
    }
    
    // ==================== Mappers ====================
    
    private OrderResponse mapToOrderResponse(PaymentOrder order, Package pkg, String paymentUrl) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .packageId(pkg != null ? pkg.getId() : null)
                .packageName(pkg != null ? pkg.getName() : null)
                .credits(pkg != null ? pkg.getCredits() : null)
                .bonusCredits(pkg != null ? pkg.getBonusCredits() : null)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .description(order.getDescription())
                .paymentUrl(paymentUrl)
                .expiresAt(LocalDateTime.now().plusMinutes(15)) // Could be stored in payment
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
    
    // ==================== Event Publishers ====================
    
    private void publishOrderCreatedEvent(PaymentOrder order, Package pkg) {
        eventPublisher.publishOrderCreated(
                order.getId(),
                order.getUserId(),
                order.getFinalAmount(),
                pkg.getId()
        );
    }
    
    private void publishPaymentInitiatedEvent(Payment payment, PaymentOrder order, Package pkg) {
        eventPublisher.publishPaymentInitiated(
                payment.getId(),
                order.getId(),
                order.getUserId(),
                payment.getAmount(),
                pkg.getCredits()
        );
    }
    
    private void publishOrderExpiredEvent(PaymentOrder order) {
        eventPublisher.publishOrderExpired(order.getId(), order.getUserId());
    }
    
    private void publishPromotionUsedEvent(String userId, String promotionCode, BigDecimal discount) {
        eventPublisher.publishPromotionUsed(userId, promotionCode, discount);
    }
}
