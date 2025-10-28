package com.example.payment_service.service;

/**
 * Email service for sending notifications to users
 * Follows auth_service email implementation pattern
 */
public interface EmailService {
    /**
     * Send welcome bonus offer email to newly registered user
     */
    void sendWelcomeBonusEmail(String to, String username);

    /**
     * Send low balance notification with promotion offers
     */
    void sendLowBalanceEmail(String to, String username, String balance);

    /**
     * Send payment confirmation email
     */
    void sendPaymentConfirmationEmail(String to, String username, String amount, String currency, String paymentId);

    /**
     * Send top-up success notification
     */
    void sendTopUpSuccessEmail(String to, String username, String amount, String currency);
}
