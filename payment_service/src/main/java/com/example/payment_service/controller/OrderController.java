package com.example.payment_service.controller;

import com.example.payment_service.dto.request.CreateOrderRequest;
import com.example.payment_service.dto.response.OrderResponse;
import com.example.payment_service.service.OrderService;
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

/**
 * Controller for payment order operations
 * Handles order creation, retrieval, and cancellation
 */
@RestController
@RequestMapping("/api/payments/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Payment order management APIs")
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping("/create")
    @Operation(summary = "Create a new order", description = "Create a new payment order with optional promotion code")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @Parameter(description = "User ID from authentication token")
            @RequestHeader(value = "X-User-ID", required = false) Long userId
    ) {
        log.info("Creating order for user: {}, package: {}", userId, request.getPackageId());
        request.setUserId(userId != null ? userId : 1L); // TODO: Get from JWT auth context
        
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieve order details by order ID")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order ID")
            @PathVariable Long orderId,
            @Parameter(description = "User ID from authentication token")
            @RequestHeader(value = "X-User-ID", required = false) Long userId
    ) {
        userId = userId != null ? userId : 1L; // TODO: Get from JWT auth context
        OrderResponse response = orderService.getOrder(orderId, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my")
    @Operation(summary = "Get my orders", description = "Retrieve all orders for the authenticated user with pagination")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @Parameter(description = "Filter by order status (PENDING, COMPLETED, FAILED, CANCELLED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "User ID from authentication token")
            @RequestHeader(value = "X-User-ID", required = false) Long userId
    ) {
        userId = userId != null ? userId : 1L; // TODO: Get from JWT auth context
        log.info("Getting orders for user: {}, status: {}, page: {}, size: {}", userId, status, page, size);
        
        Page<OrderResponse> orders = orderService.getMyOrders(
                userId, 
                status, 
                PageRequest.of(page - 1, size)
        );
        return ResponseEntity.ok(orders);
    }
    
    @DeleteMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel a pending order")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID to cancel")
            @PathVariable Long orderId,
            @Parameter(description = "User ID from authentication token")
            @RequestHeader(value = "X-User-ID", required = false) Long userId
    ) {
        userId = userId != null ? userId : 1L; // TODO: Get from JWT auth context
        log.info("Cancelling order {} for user {}", orderId, userId);
        
        OrderResponse response = orderService.cancelOrder(orderId, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/code/{orderCode}")
    @Operation(summary = "Get order by code", description = "Retrieve order details by order code")
    public ResponseEntity<OrderResponse> getOrderByCode(
            @Parameter(description = "Order code")
            @PathVariable String orderCode,
            @Parameter(description = "User ID from authentication token")
            @RequestHeader(value = "X-User-ID", required = false) Long userId
    ) {
        userId = userId != null ? userId : 1L; // TODO: Get from JWT auth context
        log.info("Getting order by code: {} for user: {}", orderCode, userId);
        
//        OrderResponse response = orderService.getOrderByCode(orderCode, userId);
        return ResponseEntity.ok(null);
    }
}
