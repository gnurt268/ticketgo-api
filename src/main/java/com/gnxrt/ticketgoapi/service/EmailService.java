package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.config.EmailConfig;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.Order;
import com.gnxrt.ticketgoapi.model.Ticket;
import com.gnxrt.ticketgoapi.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailConfig emailConfig;
    private final QRCodeService qrCodeService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Async
    public void sendOrderConfirmationEmail(Order order, List<Ticket> tickets) {
        log.info("Sending order confirmation email to: {}", order.getBuyerEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("order", order);
            context.setVariable("tickets", tickets);
            context.setVariable("event", order.getEvent());
            context.setVariable("totalAmount", formatCurrency(order.getTotalAmount()));
            context.setVariable("orderDate", order.getCreatedAt().format(DATETIME_FORMATTER));
            context.setVariable("eventDate", order.getEvent().getStartDate().format(DATE_FORMATTER));
            context.setVariable("eventTime", order.getEvent().getStartDate().format(TIME_FORMATTER));
            context.setVariable("orderDetailUrl", emailConfig.getFrontendUrl() + "/orders/" + order.getOrderCode());

            Map<String, String> ticketQRCodes = new HashMap<>();
            Map<String, byte[]> inlineImages = new HashMap<>();

            for (Ticket ticket : tickets) {
                String cid = "qr_" + ticket.getTicketCode();
                String qrContent = String.format("%s|%s|%d", ticket.getTicketCode(), ticket.getQrCode(), System.currentTimeMillis());
                byte[] qrCodeBytes = qrCodeService.generateQRCodeBytes(qrContent);

                ticketQRCodes.put(ticket.getTicketCode(), "cid:" + cid);
                inlineImages.put(cid, qrCodeBytes);
            }
            context.setVariable("ticketQRCodes", ticketQRCodes);

            String htmlContent = templateEngine.process("email/order-confirmation", context);

            sendHtmlEmailWithInlineImages(
                    order.getBuyerEmail(),
                    "Xác nhận đơn hàng #" + order.getOrderCode() + " - " + order.getEvent().getTitle(),
                    htmlContent,
                    inlineImages
            );

            log.info("Order confirmation email sent successfully to: {}", order.getBuyerEmail());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to: {}", order.getBuyerEmail(), e);
        }
    }

    @Async
    public void sendPaymentFailedEmail(Order order, String reason) {
        log.info("Sending payment failed email to: {}", order.getBuyerEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("order", order);
            context.setVariable("event", order.getEvent());
            context.setVariable("reason", reason);
            context.setVariable("retryUrl", emailConfig.getFrontendUrl() + "/orders/" + order.getOrderCode() + "/retry");

            String htmlContent = templateEngine.process("email/payment-failed", context);

            sendHtmlEmail(
                    order.getBuyerEmail(),
                    "Thanh toán thất bại - Đơn hàng #" + order.getOrderCode(),
                    htmlContent
            );

            log.info("Payment failed email sent successfully to: {}", order.getBuyerEmail());
        } catch (Exception e) {
            log.error("Failed to send payment failed email to: {}", order.getBuyerEmail(), e);
        }
    }

    @Async
    public void sendOrderCancelledEmail(Order order, String reason) {
        log.info("Sending order cancelled email to: {}", order.getBuyerEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("order", order);
            context.setVariable("event", order.getEvent());
            context.setVariable("reason", reason);
            context.setVariable("totalAmount", formatCurrency(order.getTotalAmount()));

            String htmlContent = templateEngine.process("email/order-cancelled", context);

            sendHtmlEmail(
                    order.getBuyerEmail(),
                    "Đơn hàng đã bị hủy - #" + order.getOrderCode(),
                    htmlContent
            );

            log.info("Order cancelled email sent successfully to: {}", order.getBuyerEmail());
        } catch (Exception e) {
            log.error("Failed to send order cancelled email to: {}", order.getBuyerEmail(), e);
        }
    }

    // ==================== TICKET EMAILS ====================

    @Async
    public void sendTicketEmail(Ticket ticket) {
        log.info("Sending ticket email to: {}", ticket.getHolderEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("ticket", ticket);
            context.setVariable("event", ticket.getEvent());
            context.setVariable("eventDate", ticket.getEvent().getStartDate().format(DATE_FORMATTER));
            context.setVariable("eventTime", ticket.getEvent().getStartDate().format(TIME_FORMATTER));
            context.setVariable("ticketUrl", emailConfig.getFrontendUrl() + "/tickets/" + ticket.getTicketCode());

            // Generate QR code
            String qrCodeBase64 = qrCodeService.generateTicketQRCodeBase64(ticket.getTicketCode(), ticket.getQrCode());
            context.setVariable("qrCodeBase64", qrCodeBase64);

            String htmlContent = templateEngine.process("email/ticket", context);

            sendHtmlEmail(
                    ticket.getHolderEmail(),
                    "Vé điện tử - " + ticket.getEvent().getTitle(),
                    htmlContent
            );

            log.info("Ticket email sent successfully to: {}", ticket.getHolderEmail());
        } catch (Exception e) {
            log.error("Failed to send ticket email to: {}", ticket.getHolderEmail(), e);
        }
    }

    @Async
    public void sendTicketTransferEmail(Ticket ticket, String fromEmail) {
        log.info("Sending ticket transfer email to: {}", ticket.getHolderEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("ticket", ticket);
            context.setVariable("event", ticket.getEvent());
            context.setVariable("fromEmail", fromEmail);
            context.setVariable("eventDate", ticket.getEvent().getStartDate().format(DATE_FORMATTER));
            context.setVariable("eventTime", ticket.getEvent().getStartDate().format(TIME_FORMATTER));
            context.setVariable("ticketUrl", emailConfig.getFrontendUrl() + "/tickets/" + ticket.getTicketCode());

            // Generate QR code
            String qrCodeBase64 = qrCodeService.generateTicketQRCodeBase64(ticket.getTicketCode(), ticket.getQrCode());
            context.setVariable("qrCodeBase64", qrCodeBase64);

            String htmlContent = templateEngine.process("email/ticket-transfer", context);

            sendHtmlEmail(
                    ticket.getHolderEmail(),
                    "Bạn đã nhận được vé - " + ticket.getEvent().getTitle(),
                    htmlContent
            );

            log.info("Ticket transfer email sent successfully to: {}", ticket.getHolderEmail());
        } catch (Exception e) {
            log.error("Failed to send ticket transfer email to: {}", ticket.getHolderEmail(), e);
        }
    }

    @Async
    public void sendTicketTransferNotificationEmail(String fromEmail, Ticket ticket) {
        log.info("Sending ticket transfer notification to: {}", fromEmail);

        try {
            Context context = createBaseContext();
            context.setVariable("ticket", ticket);
            context.setVariable("event", ticket.getEvent());
            context.setVariable("newHolderName", ticket.getHolderName());
            context.setVariable("newHolderEmail", ticket.getHolderEmail());

            String htmlContent = templateEngine.process("email/ticket-transfer-notification", context);

            sendHtmlEmail(
                    fromEmail,
                    "Chuyển nhượng vé thành công - " + ticket.getEvent().getTitle(),
                    htmlContent
            );

            log.info("Ticket transfer notification sent successfully to: {}", fromEmail);
        } catch (Exception e) {
            log.error("Failed to send ticket transfer notification to: {}", fromEmail, e);
        }
    }

    // ==================== EVENT EMAILS ====================

    @Async
    public void sendEventReminderEmail(Ticket ticket) {
        log.info("Sending event reminder email to: {}", ticket.getHolderEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("ticket", ticket);
            context.setVariable("event", ticket.getEvent());
            context.setVariable("eventDate", ticket.getEvent().getStartDate().format(DATE_FORMATTER));
            context.setVariable("eventTime", ticket.getEvent().getStartDate().format(TIME_FORMATTER));
            context.setVariable("ticketUrl", emailConfig.getFrontendUrl() + "/tickets/" + ticket.getTicketCode());

            // Generate QR code
            String qrCodeBase64 = qrCodeService.generateTicketQRCodeBase64(ticket.getTicketCode(), ticket.getQrCode());
            context.setVariable("qrCodeBase64", qrCodeBase64);

            String htmlContent = templateEngine.process("email/event-reminder", context);

            sendHtmlEmail(
                    ticket.getHolderEmail(),
                    "Nhắc nhở: " + ticket.getEvent().getTitle() + " sắp diễn ra!",
                    htmlContent
            );

            log.info("Event reminder email sent successfully to: {}", ticket.getHolderEmail());
        } catch (Exception e) {
            log.error("Failed to send event reminder email to: {}", ticket.getHolderEmail(), e);
        }
    }

    @Async
    public void sendEventCancelledEmail(Ticket ticket, String reason) {
        log.info("Sending event cancelled email to: {}", ticket.getHolderEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("ticket", ticket);
            context.setVariable("event", ticket.getEvent());
            context.setVariable("reason", reason);

            String htmlContent = templateEngine.process("email/event-cancelled", context);

            sendHtmlEmail(
                    ticket.getHolderEmail(),
                    "Sự kiện đã bị hủy - " + ticket.getEvent().getTitle(),
                    htmlContent
            );

            log.info("Event cancelled email sent successfully to: {}", ticket.getHolderEmail());
        } catch (Exception e) {
            log.error("Failed to send event cancelled email to: {}", ticket.getHolderEmail(), e);
        }
    }

    @Async
    public void sendEventApprovedEmail(Event event) {
        log.info("Sending event approved email to: {}", event.getOrganizer().getEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("event", event);
            context.setVariable("organizer", event.getOrganizer());
            context.setVariable("eventUrl", emailConfig.getFrontendUrl() + "/organizer/events/" + event.getId());

            String htmlContent = templateEngine.process("email/event-approved", context);

            sendHtmlEmail(
                    event.getOrganizer().getEmail(),
                    "Sự kiện đã được duyệt - " + event.getTitle(),
                    htmlContent
            );

            log.info("Event approved email sent successfully to: {}", event.getOrganizer().getEmail());
        } catch (Exception e) {
            log.error("Failed to send event approved email to: {}", event.getOrganizer().getEmail(), e);
        }
    }

    @Async
    public void sendEventRejectedEmail(Event event, String reason) {
        log.info("Sending event rejected email to: {}", event.getOrganizer().getEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("event", event);
            context.setVariable("organizer", event.getOrganizer());
            context.setVariable("reason", reason);
            context.setVariable("eventUrl", emailConfig.getFrontendUrl() + "/organizer/events/" + event.getId());

            String htmlContent = templateEngine.process("email/event-rejected", context);

            sendHtmlEmail(
                    event.getOrganizer().getEmail(),
                    "Sự kiện không được duyệt - " + event.getTitle(),
                    htmlContent
            );

            log.info("Event rejected email sent successfully to: {}", event.getOrganizer().getEmail());
        } catch (Exception e) {
            log.error("Failed to send event rejected email to: {}", event.getOrganizer().getEmail(), e);
        }
    }

    // ==================== AUTH EMAILS ====================

    @Async
    public void sendVerificationEmail(User user, String verificationToken) {
        log.info("Sending verification email to: {}", user.getEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("user", user);
            context.setVariable("verificationUrl", emailConfig.getFrontendUrl() + "/verify-email?token=" + verificationToken);

            String htmlContent = templateEngine.process("email/verify-email", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "Xác thực tài khoản TicketGo",
                    htmlContent
            );

            log.info("Verification email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendPasswordResetEmail(User user, String resetToken) {
        log.info("Sending password reset email to: {}", user.getEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("user", user);
            context.setVariable("resetUrl", emailConfig.getFrontendUrl() + "/reset-password?token=" + resetToken);

            String htmlContent = templateEngine.process("email/reset-password", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "Đặt lại mật khẩu TicketGo",
                    htmlContent
            );

            log.info("Password reset email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendWelcomeEmail(User user) {
        log.info("Sending welcome email to: {}", user.getEmail());

        try {
            Context context = createBaseContext();
            context.setVariable("user", user);
            context.setVariable("exploreUrl", emailConfig.getFrontendUrl() + "/events");

            String htmlContent = templateEngine.process("email/welcome", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "Chào mừng bạn đến với TicketGo!",
                    htmlContent
            );

            log.info("Welcome email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
        }
    }

    // ==================== HELPER METHODS ====================

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        helper.setFrom(emailConfig.getFromAddress());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private void sendHtmlEmailWithInlineImages(String to, String subject, String htmlContent,
                                               Map<String, byte[]> inlineImages) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        helper.setFrom(emailConfig.getFromAddress());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        // Add inline images
        for (Map.Entry<String, byte[]> entry : inlineImages.entrySet()) {
            helper.addInline(entry.getKey(),
                    new org.springframework.core.io.ByteArrayResource(entry.getValue()),
                    "image/png");
        }

        mailSender.send(message);
    }

    private Context createBaseContext() {
        Context context = new Context();
        context.setVariable("logoUrl", emailConfig.getLogoUrl());
        context.setVariable("frontendUrl", emailConfig.getFrontendUrl());
        context.setVariable("supportEmail", emailConfig.getSupportEmail());
        context.setVariable("hotline", emailConfig.getHotline());
        context.setVariable("currentYear", java.time.Year.now().getValue());
        return context;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return formatter.format(amount) + " ₫";
    }
}