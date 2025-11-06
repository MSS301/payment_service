package com.example.payment_service.service.impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.payment_service.service.EmailService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Email service implementation for sending notifications to users
 * Follows auth_service email implementation pattern with ObjectProvider for optional mail configuration
 * Enhanced with Circuit Breaker pattern for email service resilience
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailServiceImpl implements EmailService {

    ObjectProvider<JavaMailSender> mailSenderProvider;

    @Override
    @CircuitBreaker(name = "email-service", fallbackMethod = "sendEmailFallback")
    @Retry(name = "email-service")
    public void sendWelcomeBonusEmail(String to, String username) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.warn("JavaMailSender bean not configured. Skipping welcome email for {}", to);
                return;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = buildWelcomeBonusEmailHtml(username);

            helper.setTo(to);
            helper.setSubject("Welcome to Payment Service - Special Offers Inside! 🎉");
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
            log.info("Welcome bonus email sent to: {}", to);
        } catch (MessagingException ex) {
            log.error("Failed to send welcome email to {}: {}", to, ex.getMessage());
        }
    }

    private String buildWelcomeBonusEmailHtml(String username) {
        return "<!DOCTYPE html>" + "<html>"
                + "<head>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f4f4f4; }"
                + ".container { max-width: 600px; margin: 20px auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }"
                + ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; margin: -30px -30px 20px -30px; }"
                + ".promo-box { background: #f8f9fa; border-left: 4px solid #667eea; padding: 15px; margin: 15px 0; }"
                + ".promo-code { font-size: 18px; font-weight: bold; color: #667eea; }"
                + ".button { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; font-weight: bold; }"
                + ".button:hover { opacity: 0.9; }"
                + ".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #888; text-align: center; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<h1 style='margin: 0;'>🎉 Welcome to Payment Service!</h1>"
                + "</div>"
                + "<p>Hi <strong>"
                + username + "</strong>,</p>"
                + "<p>Thank you for joining our payment service! We're excited to have you on board.</p>"
                + "<p>As a new member, you get <strong>exclusive welcome offers</strong> on your first purchase:</p>"
                + "<div class='promo-box'>"
                + "<p style='margin: 5px 0;'>🎁 <span class='promo-code'>WELCOME10</span></p>"
                + "<p style='margin: 5px 0; font-size: 14px;'>Get <strong>10% off</strong> on any credit package</p>"
                + "</div>"
                + "<div class='promo-box'>"
                + "<p style='margin: 5px 0;'>💎 <span class='promo-code'>NEWUSER50</span></p>"
                + "<p style='margin: 5px 0; font-size: 14px;'>Save <strong>50,000 VND</strong> on orders 200,000+ VND</p>"
                + "</div>"
                + "<p>Browse our credit packages and start saving today!</p>"
                + "<div style='text-align: center;'>"
                + "<a href='http://localhost:8084/payment/packages' class='button'>View Credit Packages</a>"
                + "</div>"
                + "<p>If you have any questions, feel free to reach out to our support team.</p>"
                + "<div class='footer'>"
                + "<p>Best regards,<br><strong>Payment Service Team</strong></p>"
                + "<p>This is an automated message. Please do not reply to this email.</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    @Override
    public void sendLowBalanceEmail(String to, String username, String balance) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.warn("JavaMailSender bean not configured. Skipping low balance email for {}", to);
                return;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = buildLowBalanceEmailHtml(username, balance);

            helper.setTo(to);
            helper.setSubject("⚠️ Low Balance Alert - Special Top-Up Offers Available");
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
            log.info("Low balance email sent to: {}", to);
        } catch (MessagingException ex) {
            log.error("Failed to send low balance email to {}: {}", to, ex.getMessage());
        }
    }

    private String buildLowBalanceEmailHtml(String username, String balance) {
        return "<!DOCTYPE html>" + "<html>"
                + "<head>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f4f4f4; }"
                + ".container { max-width: 600px; margin: 20px auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }"
                + ".header { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; margin: -30px -30px 20px -30px; }"
                + ".alert-box { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 15px 0; border-radius: 4px; }"
                + ".promo-box { background: #e8f5e9; border-left: 4px solid #4caf50; padding: 15px; margin: 15px 0; }"
                + ".promo-code { font-size: 18px; font-weight: bold; color: #4caf50; }"
                + ".balance { font-size: 24px; font-weight: bold; color: #f5576c; }"
                + ".button { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; font-weight: bold; }"
                + ".button:hover { opacity: 0.9; }"
                + ".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #888; text-align: center; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<h1 style='margin: 0;'>⚠️ Low Balance Alert</h1>"
                + "</div>"
                + "<p>Hi <strong>"
                + username + "</strong>,</p>"
                + "<div class='alert-box'>"
                + "<p style='margin: 5px 0;'>Your current balance: <span class='balance'>"
                + balance + " credits</span></p>"
                + "<p style='margin: 5px 0; font-size: 14px;'>Your balance is running low. Top up now to continue using our services!</p>"
                + "</div>"
                + "<p><strong>🎁 Special Top-Up Offers Just for You:</strong></p>"
                + "<div class='promo-box'>"
                + "<p style='margin: 5px 0;'>⚡ <span class='promo-code'>FLASH25</span></p>"
                + "<p style='margin: 5px 0; font-size: 14px;'>Get <strong>25% OFF</strong> on any top-up</p>"
                + "<p style='margin: 5px 0; font-size: 12px; color: #666;'>Valid for 24 hours only!</p>"
                + "</div>"
                + "<div class='promo-box'>"
                + "<p style='margin: 5px 0;'>💎 <span class='promo-code'>VIP20</span></p>"
                + "<p style='margin: 5px 0; font-size: 14px;'>Save <strong>20% OFF</strong> on high-value packages (500,000+ VND)</p>"
                + "</div>"
                + "<div style='text-align: center;'>"
                + "<a href='http://localhost:8084/payment/packages' class='button'>Top Up Now</a>"
                + "</div>"
                + "<p style='margin-top: 20px; font-size: 14px;'>Don't miss out on these exclusive offers!</p>"
                + "<div class='footer'>"
                + "<p>Best regards,<br><strong>Payment Service Team</strong></p>"
                + "<p>This is an automated message. Please do not reply to this email.</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    @Override
    public void sendPaymentConfirmationEmail(String to, String username, String amount, String currency,
            String paymentId) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.warn("JavaMailSender bean not configured. Skipping payment confirmation email for {}", to);
                return;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = buildPaymentConfirmationEmailHtml(username, amount, currency, paymentId);

            helper.setTo(to);
            helper.setSubject("✅ Payment Successful - Order Confirmation");
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
            log.info("Payment confirmation email sent to: {}", to);
        } catch (MessagingException ex) {
            log.error("Failed to send payment confirmation email to {}: {}", to, ex.getMessage());
        }
    }

    private String buildPaymentConfirmationEmailHtml(String username, String amount, String currency,
            String paymentId) {
        return "<!DOCTYPE html>" + "<html>"
                + "<head>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f4f4f4; }"
                + ".container { max-width: 600px; margin: 20px auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }"
                + ".header { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; margin: -30px -30px 20px -30px; }"
                + ".success-icon { font-size: 48px; margin: 10px 0; }"
                + ".detail-box { background: #f8f9fa; padding: 15px; margin: 15px 0; border-radius: 4px; }"
                + ".detail-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #e9ecef; }"
                + ".detail-label { font-weight: 600; color: #666; }"
                + ".detail-value { color: #333; }"
                + ".amount { font-size: 24px; font-weight: bold; color: #11998e; }"
                + ".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #888; text-align: center; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<div class='success-icon'>✅</div>"
                + "<h1 style='margin: 0;'>Payment Successful!</h1>"
                + "</div>"
                + "<p>Hi <strong>"
                + username + "</strong>,</p>"
                + "<p>Your payment has been processed successfully. Thank you for your purchase!</p>"
                + "<div class='detail-box'>"
                + "<h3 style='margin-top: 0;'>Payment Details</h3>"
                + "<div class='detail-row'>"
                + "<span class='detail-label'>Payment ID:</span>"
                + "<span class='detail-value'>"
                + paymentId + "</span>"
                + "</div>"
                + "<div class='detail-row'>"
                + "<span class='detail-label'>Amount:</span>"
                + "<span class='amount'>"
                + amount + " " + currency + "</span>"
                + "</div>"
                + "<div class='detail-row' style='border-bottom: none;'>"
                + "<span class='detail-label'>Status:</span>"
                + "<span class='detail-value' style='color: #11998e; font-weight: bold;'>✓ Completed</span>"
                + "</div>"
                + "</div>"
                + "<p>Your credits have been added to your wallet and are ready to use!</p>"
                + "<p>If you have any questions or concerns about this transaction, please contact our support team.</p>"
                + "<div class='footer'>"
                + "<p>Best regards,<br><strong>Payment Service Team</strong></p>"
                + "<p>This is an automated message. Please do not reply to this email.</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    @Override
    public void sendTopUpSuccessEmail(String to, String username, String amount, String currency) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.warn("JavaMailSender bean not configured. Skipping top-up success email for {}", to);
                return;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = buildTopUpSuccessEmailHtml(username, amount, currency);

            helper.setTo(to);
            helper.setSubject("✨ Credits Added Successfully");
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
            log.info("Top-up success email sent to: {}", to);
        } catch (MessagingException ex) {
            log.error("Failed to send top-up success email to {}: {}", to, ex.getMessage());
        }
    }

    private String buildTopUpSuccessEmailHtml(String username, String amount, String currency) {
        return "<!DOCTYPE html>" + "<html>"
                + "<head>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f4f4f4; }"
                + ".container { max-width: 600px; margin: 20px auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }"
                + ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; margin: -30px -30px 20px -30px; }"
                + ".amount { font-size: 32px; font-weight: bold; color: #667eea; text-align: center; margin: 20px 0; }"
                + ".info-box { background: #f0f4ff; padding: 15px; margin: 15px 0; border-radius: 4px; text-align: center; }"
                + ".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #888; text-align: center; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<h1 style='margin: 0;'>✨ Credits Added!</h1>"
                + "</div>"
                + "<p>Hi <strong>"
                + username + "</strong>,</p>"
                + "<p>Great news! Your wallet has been topped up successfully.</p>"
                + "<div class='amount'>+ "
                + amount + " " + currency + "</div>"
                + "<div class='info-box'>"
                + "<p style='margin: 5px 0;'>💎 Your credits are ready to use!</p>"
                + "<p style='margin: 5px 0; font-size: 14px;'>Start using our services with your updated balance.</p>"
                + "</div>"
                + "<p>Thank you for choosing our payment service!</p>"
                + "<div class='footer'>"
                + "<p>Best regards,<br><strong>Payment Service Team</strong></p>"
                + "<p>This is an automated message. Please do not reply to this email.</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    /**
     * Fallback method for email sending failures
     * Logs the failure and continues gracefully without interrupting the main flow
     */
    private void sendEmailFallback(String to, String username, Exception ex) {
        log.error("Email service is unavailable. Failed to send email to: {}. Error: {}",
                to, ex.getMessage());
        log.warn("Email will not be sent. Consider implementing a retry queue for failed emails.");
        // In production, you might want to:
        // 1. Store failed email in database for later retry
        // 2. Send to a message queue for async retry
        // 3. Alert monitoring system
    }
}
