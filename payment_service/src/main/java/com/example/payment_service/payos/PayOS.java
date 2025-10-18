package com.example.payment_service.payos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * PayOS - Main class để tương tác với PayOS API
 */
@Slf4j
public class PayOS {
    
    private static final String PAYOS_API_URL = "https://api-merchant.payos.vn/v2/payment-requests";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final String clientId;
    private final String apiKey;
    private final String checksumKey;
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    public PayOS(String clientId, String apiKey, String checksumKey) {
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.checksumKey = checksumKey;
        this.httpClient = new OkHttpClient();
        this.gson = new GsonBuilder().create();
    }
    
    /**
     * Tạo payment link
     */
    public CheckoutResponseData createPaymentLink(PaymentData paymentData) throws Exception {
        try {
            // Create request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("orderCode", paymentData.getOrderCode());
            requestBody.put("amount", paymentData.getAmount());
            requestBody.put("description", paymentData.getDescription());
            requestBody.put("items", paymentData.getItems());
            requestBody.put("cancelUrl", paymentData.getCancelUrl());
            requestBody.put("returnUrl", paymentData.getReturnUrl());
            
            // Generate signature
            String signature = generateSignature(requestBody);
            requestBody.put("signature", signature);
            
            String json = gson.toJson(requestBody);
            
            Request request = new Request.Builder()
                    .url(PAYOS_API_URL)
                    .post(RequestBody.create(json, JSON))
                    .addHeader("x-client-id", clientId)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("PayOS API error: {} - {}", response.code(), errorBody);
                    throw new Exception("PayOS API error: " + response.code() + " - " + errorBody);
                }
                
                String responseBody = response.body().string();
                log.debug("PayOS createPaymentLink response: {}", responseBody);
                
                Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
                Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                
                return CheckoutResponseData.builder()
                        .bin((String) data.get("bin"))
                        .accountNumber((String) data.get("accountNumber"))
                        .accountName((String) data.get("accountName"))
                        .amount(((Double) data.get("amount")).intValue())
                        .description((String) data.get("description"))
                        .orderCode(((Double) data.get("orderCode")).longValue())
                        .currency((String) data.get("currency"))
                        .paymentLinkId((String) data.get("paymentLinkId"))
                        .status((String) data.get("status"))
                        .checkoutUrl((String) data.get("checkoutUrl"))
                        .qrCode((String) data.get("qrCode"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Error creating payment link: ", e);
            throw e;
        }
    }
    
    /**
     * Lấy thông tin payment link
     */
    public PaymentLinkData getPaymentLinkInformation(long orderCode) throws Exception {
        try {
            String url = PAYOS_API_URL + "/" + orderCode;
            
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("x-client-id", clientId)
                    .addHeader("x-api-key", apiKey)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("PayOS API error: {} - {}", response.code(), errorBody);
                    throw new Exception("PayOS API error: " + response.code());
                }
                
                String responseBody = response.body().string();
                log.debug("PayOS getPaymentLinkInformation response: {}", responseBody);
                
                Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
                Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                
                return PaymentLinkData.builder()
                        .id((String) data.get("id"))
                        .orderCode(((Double) data.get("orderCode")).longValue())
                        .amount(((Double) data.get("amount")).intValue())
                        .amountPaid(((Double) data.getOrDefault("amountPaid", 0.0)).intValue())
                        .amountRemaining(((Double) data.getOrDefault("amountRemaining", 0.0)).intValue())
                        .status((String) data.get("status"))
                        .createdAt((String) data.get("createdAt"))
                        .cancellationReason((String) data.get("cancellationReason"))
                        .canceledAt((String) data.get("canceledAt"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Error getting payment link information: ", e);
            throw e;
        }
    }
    
    /**
     * Hủy payment link
     */
    public PaymentLinkData cancelPaymentLink(long orderCode, String cancellationReason) throws Exception {
        try {
            String url = PAYOS_API_URL + "/" + orderCode;
            
            Map<String, Object> requestBody = new HashMap<>();
            if (cancellationReason != null && !cancellationReason.isEmpty()) {
                requestBody.put("cancellationReason", cancellationReason);
            }
            
            String json = gson.toJson(requestBody);
            
            Request request = new Request.Builder()
                    .url(url)
                    .delete(RequestBody.create(json, JSON))
                    .addHeader("x-client-id", clientId)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("PayOS API error: {} - {}", response.code(), errorBody);
                    throw new Exception("PayOS API error: " + response.code());
                }
                
                String responseBody = response.body().string();
                log.debug("PayOS cancelPaymentLink response: {}", responseBody);
                
                Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
                Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                
                return PaymentLinkData.builder()
                        .id((String) data.get("id"))
                        .orderCode(((Double) data.get("orderCode")).longValue())
                        .amount(((Double) data.get("amount")).intValue())
                        .status((String) data.get("status"))
                        .cancellationReason((String) data.get("cancellationReason"))
                        .canceledAt((String) data.get("canceledAt"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Error cancelling payment link: ", e);
            throw e;
        }
    }
    
    /**
     * Xác thực webhook URL
     */
    public String confirmWebhook(String webhookUrl) throws Exception {
        try {
            String url = "https://api-merchant.payos.vn/v2/confirm-webhook";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("webhookUrl", webhookUrl);
            
            String json = gson.toJson(requestBody);
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(json, JSON))
                    .addHeader("x-client-id", clientId)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("PayOS API error: {} - {}", response.code(), errorBody);
                    throw new Exception("PayOS API error: " + response.code());
                }
                
                String responseBody = response.body().string();
                log.debug("PayOS confirmWebhook response: {}", responseBody);
                
                Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
                return (String) responseMap.get("message");
            }
        } catch (Exception e) {
            log.error("Error confirming webhook: ", e);
            throw e;
        }
    }
    
    /**
     * Xác minh dữ liệu webhook
     */
    public WebhookData verifyPaymentWebhookData(Webhook webhookBody) throws Exception {
        try {
            // Verify signature
            String receivedSignature = webhookBody.getSignature();
            WebhookData data = webhookBody.getData();
            
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("orderCode", data.getOrderCode());
            dataMap.put("amount", data.getAmount());
            dataMap.put("description", data.getDescription());
            dataMap.put("accountNumber", data.getAccountNumber());
            dataMap.put("reference", data.getReference());
            dataMap.put("transactionDateTime", data.getTransactionDateTime());
            
            String calculatedSignature = generateSignature(dataMap);
            
            if (!calculatedSignature.equals(receivedSignature)) {
                log.error("Webhook signature verification failed");
                throw new Exception("Invalid webhook signature");
            }
            
            log.info("Webhook signature verified successfully");
            return data;
            
        } catch (Exception e) {
            log.error("Error verifying webhook data: ", e);
            throw e;
        }
    }
    
    /**
     * Generate signature cho request
     */
    private String generateSignature(Map<String, Object> data) throws Exception {
        try {
            // Sort keys
            TreeMap<String, Object> sortedData = new TreeMap<>(data);
            StringBuilder signatureData = new StringBuilder();
            
            sortedData.forEach((key, value) -> {
                if (value != null && !key.equals("signature")) {
                    signatureData.append(key).append("=").append(value).append("&");
                }
            });
            
            // Remove last &
            if (signatureData.length() > 0) {
                signatureData.setLength(signatureData.length() - 1);
            }
            
            // Generate HMAC SHA256
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(
                    checksumKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            sha256_HMAC.init(secret_key);
            
            byte[] hash = sha256_HMAC.doFinal(signatureData.toString().getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            log.error("Error generating signature: ", e);
            throw e;
        }
    }
}