package com.lexoratech.lexorafix.network;

import android.util.Log;

import com.lexoratech.lexorafix.BuildConfig;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Sends emails via Gmail SMTP using a Google App Password.
 * Credentials are read from BuildConfig (set in local.properties).
 *
 * Usage:
 * EmailHelper.getInstance().sendOrderConfirmation(email, orderId, total,
 * itemsHtml);
 */
public class EmailHelper {

    private static final String TAG = "EmailHelper";
    private static EmailHelper instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Session session;

    private EmailHelper() {
        String senderEmail = BuildConfig.EMAIL_SENDER_ADDRESS;
        String appPassword = BuildConfig.EMAIL_APP_PASSWORD;

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2"); // Fix for Modern Android Handshake failures

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, appPassword);
            }
        });
    }

    public static synchronized EmailHelper getInstance() {
        if (instance == null) {
            instance = new EmailHelper();
        }
        return instance;
    }

    /**
     * Send order confirmation email to customer.
     */
    public void sendOrderConfirmation(String toEmail, String orderId, double total, String itemsHtml) {
        String subject = "✅ Order Confirmed — LexoraFix #" + orderId;
        String body = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body style='font-family:Arial,sans-serif;margin:0;padding:0;background:#f5f5f5;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.1);'>"
                + "<div style='background:linear-gradient(135deg,#2563EB,#06B6D4);padding:32px;text-align:center;'>"
                + "<h1 style='color:#fff;margin:0;font-size:24px;'>🎉 Order Confirmed!</h1>"
                + "<p style='color:rgba(255,255,255,0.9);margin:8px 0 0;'>Order #" + orderId + "</p></div>"
                + "<div style='padding:24px;'>"
                + "<h3 style='color:#1A1A2E;margin-top:0;'>Order Summary</h3>"
                + (itemsHtml != null ? itemsHtml : "")
                + "<div style='border-top:2px solid #2563EB;margin-top:16px;padding-top:16px;text-align:right;'>"
                + "<h2 style='color:#2563EB;margin:0;'>Total: LKR " + String.format("%.2f", total) + "</h2></div>"
                + "<p style='color:#6B7280;font-size:14px;margin-top:24px;'>Thank you for shopping with LexoraFix! Your order is being processed and you'll receive updates soon.</p>"
                + "</div>"
                + "<div style='background:#f9fafb;padding:16px;text-align:center;border-top:1px solid #e5e7eb;'>"
                + "<p style='color:#9CA3AF;margin:0;font-size:12px;'>© LexoraFix by LexoraTech</p></div>"
                + "</div></body></html>";

        sendEmail(toEmail, subject, body);
    }

    /**
     * Send welcome email to newly registered users.
     */
    public void sendWelcomeEmail(String toEmail, String userName) {
        String subject = "👋 Welcome to LexoraFix, " + (userName != null && !userName.isEmpty() ? userName : "there")
                + "!";
        String body = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body style='font-family:Arial,sans-serif;margin:0;padding:20px;background:#f4f4f9;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.05);'>"
                + "<div style='background:linear-gradient(135deg,#2563EB,#06B6D4);padding:40px 32px;text-align:center;'>"
                + "<h1 style='color:#fff;margin:0;font-size:28px;letter-spacing:0.5px;'>Welcome to LexoraFix! 🚀</h1>"
                + "<p style='color:rgba(255,255,255,0.95);margin:12px 0 0;font-size:16px;'>Your premier destination for top-tier products & services.</p></div>"
                + "<div style='padding:40px 32px;text-align:left;'>"
                + "<h2 style='color:#1A1A2E;margin-top:0;font-size:22px;'>Hi " + safe(userName) + ",</h2>"
                + "<p style='color:#4B5563;font-size:16px;line-height:1.7;margin-bottom:32px;'>"
                + "We're absolutely thrilled to have you here. With LexoraFix, you can explore hundreds of exclusive products and access certified professional services right from the palm of your hand.</p>"
                + "<div style='background:#fcfcfd;border-radius:12px;padding:28px;margin-bottom:32px;border:1px solid #f0f0f4;box-shadow:inset 0 2px 4px rgba(0,0,0,0.02);'>"
                + "<h3 style='color:#2563EB;margin:0 0 20px;font-size:18px;'>Ready to get started?</h3>"
                + "<div style='margin-bottom:12px;display:flex;align-items:center;'><span style='margin-right:12px;font-size:18px;'>✨</span><span style='color:#374151;font-size:15px;line-height:1.5;font-weight:500;'>Browse our latest amazing Deals & Offers</span></div>"
                + "<div style='margin-bottom:12px;display:flex;align-items:center;'><span style='margin-right:12px;font-size:18px;'>📝</span><span style='color:#374151;font-size:15px;line-height:1.5;font-weight:500;'>Set up your speedy delivery address in Profile</span></div>"
                + "<div style='display:flex;align-items:center;'><span style='margin-right:12px;font-size:18px;'>💬</span><span style='color:#374151;font-size:15px;line-height:1.5;font-weight:500;'>Connect with verified & certified Service Providers</span></div>"
                + "</div>"
                + "<p style='color:#6B7280;font-size:14px;line-height:1.6;'>If you have any questions, simply reply to this email or contact our support team directly through the app.</p>"
                + "</div>"
                + "<div style='background:#1A1A2E;padding:24px;text-align:center;border-top:1px solid #2A2A3E;'>"
                + "<p style='color:#9CA3AF;margin:0;font-size:13px;letter-spacing:0.5px;'>© LexoraFix by LexoraTech<br><span style='color:#6B7280;font-size:12px;display:block;margin-top:4px;'>Colombo, Sri Lanka</span></p></div>"
                + "</div></body></html>";

        sendEmail(toEmail, subject, body);
    }

    /**
     * Send rich order notification to admin/seller with full details.
     */
    public void sendAdminOrderNotification(String orderId, String customerName, String customerEmail,
            String customerPhone, String address, String itemsHtml, double total) {
        String adminEmail = BuildConfig.ADMIN_EMAIL;
        if (adminEmail == null || adminEmail.isEmpty())
            return;

        String subject = "🛒 New Order #" + orderId + " — LexoraFix (LKR " + String.format("%.2f", total) + ")";
        String body = "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                + "<body style='font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#1A1A2E;border-radius:12px;overflow:hidden;'>"
                // Header
                + "<div style='background:linear-gradient(135deg,#2563EB,#06B6D4);padding:28px;text-align:center;'>"
                + "<h1 style='color:#fff;margin:0;font-size:22px;'>🛒 New Order Received!</h1>"
                + "<p style='color:rgba(255,255,255,0.9);margin:8px 0 0;font-size:16px;'>Order #" + orderId
                + "</p></div>"
                // Customer info
                + "<div style='padding:24px;'>"
                + "<h3 style='color:#2563EB;margin:0 0 12px;font-size:15px;'>👤 Customer Details</h3>"
                + "<div style='background:#242443;border-radius:8px;padding:16px;margin-bottom:20px;'>"
                + "<table style='width:100%;border-collapse:collapse;'>"
                + "<tr><td style='color:#9CA3AF;padding:4px 0;width:100px;'>Name</td>"
                + "<td style='color:#E0E0E0;font-weight:bold;'>" + safe(customerName) + "</td></tr>"
                + "<tr><td style='color:#9CA3AF;padding:4px 0;'>Email</td>"
                + "<td style='color:#E0E0E0;'>" + safe(customerEmail) + "</td></tr>"
                + "<tr><td style='color:#9CA3AF;padding:4px 0;'>Phone</td>"
                + "<td style='color:#E0E0E0;'>" + safe(customerPhone) + "</td></tr>"
                + "<tr><td style='color:#9CA3AF;padding:4px 0;'>Address</td>"
                + "<td style='color:#E0E0E0;'>" + safe(address) + "</td></tr>"
                + "</table></div>"
                // Order items
                + "<h3 style='color:#2563EB;margin:0 0 12px;font-size:15px;'>📦 Order Items</h3>"
                + "<div style='background:#242443;border-radius:8px;padding:16px;margin-bottom:20px;'>"
                + (itemsHtml != null ? itemsHtml : "<p style='color:#9CA3AF;'>No items</p>")
                + "</div>"
                // Total
                + "<div style='background:#0D3B1E;border:1px solid #10B981;border-radius:8px;padding:16px;text-align:center;'>"
                + "<p style='color:#9CA3AF;margin:0 0 4px;font-size:13px;'>Total Amount</p>"
                + "<h2 style='color:#10B981;margin:0;font-size:24px;'>LKR " + String.format("%.2f", total) + "</h2>"
                + "<p style='color:#6B7280;margin:4px 0 0;font-size:12px;'>💳 PayHere — Paid</p></div>"
                // Footer
                + "<p style='color:#6B7280;margin:20px 0 0;font-size:12px;text-align:center;'>"
                + "Open the LexoraFix Admin app to manage this order.</p>"
                + "</div></div></body></html>";

        sendEmail(adminEmail, subject, body);
    }

    /**
     * Simple admin notification (backward compatible — orderId, name, total only).
     */
    public void sendAdminOrderNotificationSimple(String orderId, String customerName, double total) {
        sendAdminOrderNotification(orderId, customerName, "", "", "", null, total);
    }

    /**
     * Send notification to admin when a new product review is submitted.
     */
    public void sendAdminNewReviewNotification(String productName, String customerName, float rating,
            String reviewText) {
        String adminEmail = BuildConfig.ADMIN_EMAIL;
        if (adminEmail == null || adminEmail.isEmpty())
            return;

        String subject = "⭐ New Review for " + productName + " — LexoraFix";
        String body = "<!DOCTYPE html><html><head><meta charset='utf-8'></head>"
                + "<body style='font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#1A1A2E;border-radius:12px;overflow:hidden;'>"
                + "<div style='background:linear-gradient(135deg,#2563EB,#06B6D4);padding:28px;text-align:center;'>"
                + "<h1 style='color:#fff;margin:0;font-size:22px;'>⭐ New Product Review</h1>"
                + "<p style='color:rgba(255,255,255,0.9);margin:8px 0 0;font-size:16px;'>" + safe(productName)
                + "</p></div>"
                + "<div style='padding:24px;'>"
                + "<div style='background:#242443;border-radius:8px;padding:16px;margin-bottom:20px;'>"
                + "<p style='color:#9CA3AF;margin:0 0 8px;'><strong>Customer:</strong> <span style='color:#E0E0E0;'>"
                + safe(customerName) + "</span></p>"
                + "<p style='color:#9CA3AF;margin:0 0 8px;'><strong>Rating:</strong> <span style='color:#FFD700;'>"
                + rating + " / 5.0</span></p>"
                + "<p style='color:#9CA3AF;margin:12px 0 4px;'><strong>Review:</strong></p>"
                + "<p style='color:#E0E0E0;margin:0;font-style:italic;'>\"" + safe(reviewText) + "\"</p>"
                + "</div>"
                + "<p style='color:#6B7280;margin:20px 0 0;font-size:12px;text-align:center;'>"
                + "Open the LexoraFix Admin app to manage this review.</p>"
                + "</div></div></body></html>";

        sendEmail(adminEmail, subject, body);
    }

    private String safe(String s) {
        return s != null && !s.isEmpty() ? s : "—";
    }

    /**
     * Send tracking update email to customer.
     */
    public void sendTrackingUpdate(String toEmail, String orderId, String trackingNumber) {
        String subject = "📦 Your Order #" + orderId + " Has Been Shipped!";
        String body = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body style='font-family:Arial,sans-serif;background:#f5f5f5;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.1);'>"
                + "<div style='background:linear-gradient(135deg,#00C9A7,#4DDDC4);padding:32px;text-align:center;'>"
                + "<h1 style='color:#fff;margin:0;font-size:24px;'>📦 Order Shipped!</h1></div>"
                + "<div style='padding:24px;text-align:center;'>"
                + "<p style='color:#1A1A2E;font-size:16px;'>Your order <strong>#" + orderId
                + "</strong> is on its way!</p>"
                + "<div style='background:#f0fdf4;border:2px solid #10B981;border-radius:12px;padding:20px;margin:16px 0;'>"
                + "<p style='color:#6B7280;margin:0 0 8px;font-size:14px;'>Tracking Number</p>"
                + "<h2 style='color:#1A1A2E;margin:0;letter-spacing:2px;'>" + trackingNumber + "</h2></div>"
                + "<p style='color:#6B7280;font-size:14px;'>You can track your order in the LexoraFix app.</p>"
                + "</div></div></body></html>";

        sendEmail(toEmail, subject, body);
    }

    /**
     * Send email about account ban status update.
     */
    public void sendBanStatusEmail(String toEmail, String userName, boolean isBanned) {
        String statusText = isBanned ? "has been Restrained" : "has been Restored";
        String subject = isBanned ? "⚠️ Urgent: Your LexoraFix Account has been Restricted"
                : "✅ Your LexoraFix Account has been Restored";
        String color = isBanned ? "#EF5350" : "#10B981";

        String body = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body style='font-family:Arial,sans-serif;background:#f5f5f5;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.1);'>"
                + "<div style='background:" + color + ";padding:32px;text-align:center;'>"
                + "<h1 style='color:#fff;margin:0;font-size:24px;'>Account Status Update</h1></div>"
                + "<div style='padding:24px;'>"
                + "<h3 style='color:#1A1A2E;margin-top:0;'>Hi " + safe(userName) + ",</h3>"
                + "<p style='color:#4B5563;font-size:16px;line-height:1.6;'>"
                + "We are writing to inform you that your LexoraFix account <strong>" + statusText + "</strong>.</p>"
                + (isBanned
                        ? "<p style='color:#4B5563;font-size:16px;line-height:1.6;'>This means you can no longer place new orders, request services, or act as a seller. If you believe this is a mistake, please reply to this email to contact our dispute division.</p>"
                        : "<p style='color:#4B5563;font-size:16px;line-height:1.6;'>You now have full access to your account again. You may resume placing orders and offering services.</p>")
                + "<p style='color:#6B7280;font-size:14px;margin-top:24px;'>Thank you,<br>LexoraFix Safety Team</p>"
                + "</div></div></body></html>";

        sendEmail(toEmail, subject, body);
    }

    /**
     * Send email about successful profile update.
     */
    public void sendProfileUpdateEmail(String toEmail, String userName) {
        String subject = "🔒 Security Alert: Your Profile was Updated";
        String body = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body style='font-family:Arial,sans-serif;background:#f5f5f5;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.1);'>"
                + "<div style='background:linear-gradient(135deg,#6A1B9A,#8E24AA);padding:32px;text-align:center;'>"
                + "<h1 style='color:#fff;margin:0;font-size:24px;'>Profile Updated</h1></div>"
                + "<div style='padding:24px;'>"
                + "<p style='color:#1A1A2E;font-size:16px;'>Hi <strong>" + safe(userName) + "</strong>,</p>"
                + "<p style='color:#4B5563;font-size:16px;line-height:1.6;'>The personal information (such as your address, phone number, or photo) associated with your LexoraFix account was just updated.</p>"
                + "<div style='background:#fefce8;border-left:4px solid #eab308;padding:16px;margin:24px 0;'>"
                + "<p style='color:#854d0e;margin:0;font-size:14px;'><strong>Didn't do this?</strong> If you did not authorize this change, please contact LexoraFix support immediately to secure your account.</p></div>"
                + "<p style='color:#6B7280;font-size:14px;'>If you made this change, you can safely ignore this email.</p>"
                + "</div></div></body></html>";

        sendEmail(toEmail, subject, body);
    }

    /**
     * Internal: send email on background thread.
     */
    private void sendEmail(String toEmail, String subject, String htmlBody) {
        if (toEmail == null || toEmail.isEmpty()) {
            Log.w(TAG, "No recipient email, skipping");
            return;
        }

        String senderEmail = BuildConfig.EMAIL_SENDER_ADDRESS;
        if (senderEmail == null || senderEmail.isEmpty()) {
            Log.w(TAG, "No sender email configured, skipping");
            return;
        }

        executor.execute(() -> {
            // Fix for Android "NoSuchProviderException: smtp" on background threads
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            try {
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(senderEmail, "LexoraFix"));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
                message.setSubject(subject);

                // Anti-Spam Headers
                message.setSentDate(new java.util.Date());
                message.setHeader("X-Mailer", "LexoraFix System Mailer");
                message.setHeader("Precedence", "bulk");
                message.setHeader("List-Unsubscribe", "<mailto:" + senderEmail + "?subject=unsubscribe>");
                message.setHeader("Message-ID", "<" + java.util.UUID.randomUUID().toString() + "@lexorafix.com>");

                message.setContent(htmlBody, "text/html; charset=utf-8");
                Transport.send(message);
                Log.d(TAG, "✅ Email sent to " + toEmail);
            } catch (Exception e) {
                Log.e(TAG, "❌ Email send failed: " + e.getMessage(), e);
            }
        });
    }
}
