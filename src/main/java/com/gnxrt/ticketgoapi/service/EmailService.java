package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.config.EmailConfig;
import com.gnxrt.ticketgoapi.dto.event.EmailEvent;
import com.gnxrt.ticketgoapi.enums.EmailType;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.Order;
import com.gnxrt.ticketgoapi.model.OrganizerRequest;
import com.gnxrt.ticketgoapi.model.Payment;
import com.gnxrt.ticketgoapi.model.Ticket;
import com.gnxrt.ticketgoapi.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailProducer emailProducer;
    private final EmailConfig emailConfig;
    private final QRCodeService qrCodeService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ==================== ORDER EMAILS ====================

    public void sendOrderConfirmationEmail(Order order, List<Ticket> tickets) {
        log.info("Publishing order confirmation email event for: {}", order.getBuyerEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("order", order);
            variables.put("tickets", tickets);
            variables.put("event", order.getEvent());
            variables.put("totalAmount", formatCurrency(order.getTotalAmount()));
            variables.put("orderDate", order.getCreatedAt().format(DATETIME_FORMATTER));
            variables.put("eventDate", order.getEvent().getStartDate().format(DATE_FORMATTER));
            variables.put("eventTime", order.getEvent().getStartDate().format(TIME_FORMATTER));
            variables.put("orderDetailUrl", emailConfig.getFrontendUrl() + "/orders/" + order.getOrderCode());

            Map<String, String> ticketQRCodes = new HashMap<>();
            Map<String, byte[]> inlineImages = new HashMap<>();

            for (Ticket ticket : tickets) {
                String cid = "qr_" + ticket.getTicketCode();
                String qrContent = String.format("%s|%s|%d", ticket.getTicketCode(), ticket.getQrCode(), System.currentTimeMillis());
                byte[] qrCodeBytes = qrCodeService.generateQRCodeBytes(qrContent);

                ticketQRCodes.put(ticket.getTicketCode(), "cid:" + cid);
                inlineImages.put(cid, qrCodeBytes);
            }
            variables.put("ticketQRCodes", ticketQRCodes);

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.ORDER_CONFIRMATION)
                    .to(order.getBuyerEmail())
                    .subject("Xác nhận đơn hàng #" + order.getOrderCode() + " - " + order.getEvent().getTitle())
                    .templateName("email/order-confirmation")
                    .templateVariables(variables)
                    .inlineImages(inlineImages)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish order confirmation email event for: {}", order.getBuyerEmail(), e);
        }
    }

    public void sendPaymentFailedEmail(Order order, String reason) {
        log.info("Publishing payment failed email event for: {}", order.getBuyerEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("order", order);
            variables.put("event", order.getEvent());
            variables.put("reason", reason);
            variables.put("retryUrl", emailConfig.getFrontendUrl() + "/orders/" + order.getOrderCode() + "/retry");

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.PAYMENT_FAILED)
                    .to(order.getBuyerEmail())
                    .subject("Thanh toán thất bại - Đơn hàng #" + order.getOrderCode())
                    .templateName("email/payment-failed")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish payment failed email event for: {}", order.getBuyerEmail(), e);
        }
    }

    public void sendOrderCancelledEmail(Order order, String reason) {
        log.info("Publishing order cancelled email event for: {}", order.getBuyerEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("order", order);
            variables.put("event", order.getEvent());
            variables.put("reason", reason);
            variables.put("totalAmount", formatCurrency(order.getTotalAmount()));

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.ORDER_CANCELLED)
                    .to(order.getBuyerEmail())
                    .subject("Đơn hàng đã bị hủy - #" + order.getOrderCode())
                    .templateName("email/order-cancelled")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish order cancelled email event for: {}", order.getBuyerEmail(), e);
        }
    }

    // ==================== TICKET EMAILS ====================

    public void sendTicketEmail(Ticket ticket) {
        log.info("Publishing ticket email event for: {}", ticket.getHolderEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("ticket", ticket);
            variables.put("event", ticket.getEvent());
            variables.put("eventDate", ticket.getEvent().getStartDate().format(DATE_FORMATTER));
            variables.put("eventTime", ticket.getEvent().getStartDate().format(TIME_FORMATTER));
            variables.put("ticketUrl", emailConfig.getFrontendUrl() + "/tickets/" + ticket.getTicketCode());

            String qrCodeBase64 = qrCodeService.generateTicketQRCodeBase64(ticket.getTicketCode(), ticket.getQrCode());
            variables.put("qrCodeBase64", qrCodeBase64);

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.TICKET)
                    .to(ticket.getHolderEmail())
                    .subject("Vé điện tử - " + ticket.getEvent().getTitle())
                    .templateName("email/ticket")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish ticket email event for: {}", ticket.getHolderEmail(), e);
        }
    }

    public void sendTicketTransferEmail(Ticket ticket, String fromEmail) {
        log.info("Publishing ticket transfer email event for: {}", ticket.getHolderEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("ticket", ticket);
            variables.put("event", ticket.getEvent());
            variables.put("fromEmail", fromEmail);
            variables.put("eventDate", ticket.getEvent().getStartDate().format(DATE_FORMATTER));
            variables.put("eventTime", ticket.getEvent().getStartDate().format(TIME_FORMATTER));
            variables.put("ticketUrl", emailConfig.getFrontendUrl() + "/tickets/" + ticket.getTicketCode());

            String qrCodeBase64 = qrCodeService.generateTicketQRCodeBase64(ticket.getTicketCode(), ticket.getQrCode());
            variables.put("qrCodeBase64", qrCodeBase64);

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.TICKET_TRANSFER)
                    .to(ticket.getHolderEmail())
                    .subject("Bạn đã nhận được vé - " + ticket.getEvent().getTitle())
                    .templateName("email/ticket-transfer")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish ticket transfer email event for: {}", ticket.getHolderEmail(), e);
        }
    }

    public void sendTicketTransferNotificationEmail(String fromEmail, Ticket ticket) {
        log.info("Publishing ticket transfer notification event for: {}", fromEmail);

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("ticket", ticket);
            variables.put("event", ticket.getEvent());
            variables.put("newHolderName", ticket.getHolderName());
            variables.put("newHolderEmail", ticket.getHolderEmail());

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.TICKET_TRANSFER_NOTIFICATION)
                    .to(fromEmail)
                    .subject("Chuyển nhượng vé thành công - " + ticket.getEvent().getTitle())
                    .templateName("email/ticket-transfer-notification")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish ticket transfer notification event for: {}", fromEmail, e);
        }
    }

    // ==================== EVENT EMAILS ====================

    public void sendEventReminderEmail(Ticket ticket) {
        log.info("Publishing event reminder email event for: {}", ticket.getHolderEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("ticket", ticket);
            variables.put("event", ticket.getEvent());
            variables.put("eventDate", ticket.getEvent().getStartDate().format(DATE_FORMATTER));
            variables.put("eventTime", ticket.getEvent().getStartDate().format(TIME_FORMATTER));
            variables.put("ticketUrl", emailConfig.getFrontendUrl() + "/tickets/" + ticket.getTicketCode());

            String qrCodeBase64 = qrCodeService.generateTicketQRCodeBase64(ticket.getTicketCode(), ticket.getQrCode());
            variables.put("qrCodeBase64", qrCodeBase64);

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.EVENT_REMINDER)
                    .to(ticket.getHolderEmail())
                    .subject("Nhắc nhở: " + ticket.getEvent().getTitle() + " sắp diễn ra!")
                    .templateName("email/event-reminder")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish event reminder email event for: {}", ticket.getHolderEmail(), e);
        }
    }

    public void sendOrderRefundedEmail(Order order, Payment payment, String reason) {
        log.info("Publishing order refunded email event for: {}", order.getBuyerEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("order", order);
            variables.put("event", order.getEvent());
            variables.put("refundAmount", formatCurrency(payment.getRefundAmount()));
            variables.put("refundTransactionId", payment.getRefundTransactionId());
            variables.put("refundedAt", payment.getRefundedAt() != null
                    ? payment.getRefundedAt().format(DATETIME_FORMATTER) : "");
            variables.put("reason", reason);
            variables.put("orderDetailUrl", emailConfig.getFrontendUrl() + "/orders/" + order.getOrderCode());

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.ORDER_REFUNDED)
                    .to(order.getBuyerEmail())
                    .subject("Hoàn tiền đơn hàng #" + order.getOrderCode())
                    .templateName("email/order-refunded")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish refund email event for: {}", order.getBuyerEmail(), e);
        }
    }

    public void sendEventCancelledEmail(Ticket ticket, String reason) {
        log.info("Publishing event cancelled email event for: {}", ticket.getHolderEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("ticket", ticket);
            variables.put("event", ticket.getEvent());
            variables.put("reason", reason);

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.EVENT_CANCELLED)
                    .to(ticket.getHolderEmail())
                    .subject("Sự kiện đã bị hủy - " + ticket.getEvent().getTitle())
                    .templateName("email/event-cancelled")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish event cancelled email event for: {}", ticket.getHolderEmail(), e);
        }
    }

    public void sendEventApprovedEmail(Event evt) {
        log.info("Publishing event approved email event for: {}", evt.getOrganizer().getEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("event", evt);
            variables.put("organizer", evt.getOrganizer());
            variables.put("eventUrl", emailConfig.getFrontendUrl() + "/organizer/events/" + evt.getId());

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.EVENT_APPROVED)
                    .to(evt.getOrganizer().getEmail())
                    .subject("Sự kiện đã được duyệt - " + evt.getTitle())
                    .templateName("email/event-approved")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish event approved email event for: {}", evt.getOrganizer().getEmail(), e);
        }
    }

    public void sendEventRejectedEmail(Event evt, String reason) {
        log.info("Publishing event rejected email event for: {}", evt.getOrganizer().getEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("event", evt);
            variables.put("organizer", evt.getOrganizer());
            variables.put("reason", reason);
            variables.put("eventUrl", emailConfig.getFrontendUrl() + "/organizer/events/" + evt.getId());

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.EVENT_REJECTED)
                    .to(evt.getOrganizer().getEmail())
                    .subject("Sự kiện không được duyệt - " + evt.getTitle())
                    .templateName("email/event-rejected")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish event rejected email event for: {}", evt.getOrganizer().getEmail(), e);
        }
    }

    // ==================== AUTH EMAILS ====================

    public void sendVerificationEmail(User user, String verificationToken) {
        log.info("Publishing verification email event for: {}", user.getEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("user", user);
            variables.put("verificationUrl", emailConfig.getFrontendUrl() + "/verify-email?token=" + verificationToken);

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.VERIFICATION)
                    .to(user.getEmail())
                    .subject("Xác thực tài khoản TicketGo")
                    .templateName("email/verify-email")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish verification email event for: {}", user.getEmail(), e);
        }
    }

    public void sendPasswordResetEmail(User user, String resetToken) {
        log.info("Publishing password reset email event for: {}", user.getEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("user", user);
            variables.put("resetUrl", emailConfig.getFrontendUrl() + "/reset-password?token=" + resetToken);

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.PASSWORD_RESET)
                    .to(user.getEmail())
                    .subject("Đặt lại mật khẩu TicketGo")
                    .templateName("email/reset-password")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish password reset email event for: {}", user.getEmail(), e);
        }
    }

    public void sendWelcomeEmail(User user) {
        log.info("Publishing welcome email event for: {}", user.getEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("user", user);
            variables.put("exploreUrl", emailConfig.getFrontendUrl() + "/events");

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.WELCOME)
                    .to(user.getEmail())
                    .subject("Chào mừng bạn đến với TicketGo!")
                    .templateName("email/welcome")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish welcome email event for: {}", user.getEmail(), e);
        }
    }

    // ==================== ORGANIZER REQUEST EMAILS ====================

    public void sendOrganizerRequestReceivedEmail(User user, OrganizerRequest request) {
        log.info("Publishing organizer request received email event for: {}", user.getEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("user", user);
            variables.put("request", request);
            variables.put("requestDate", request.getCreatedAt().format(DATETIME_FORMATTER));

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.ORGANIZER_REQUEST_RECEIVED)
                    .to(user.getEmail())
                    .subject("Đã nhận yêu cầu đăng ký Organizer - TicketGo")
                    .templateName("email/organizer-request-received")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish organizer request received email event for: {}", user.getEmail(), e);
        }
    }

    public void sendOrganizerRequestApprovedEmail(User user, OrganizerRequest request) {
        log.info("Publishing organizer request approved email event for: {}", user.getEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("user", user);
            variables.put("request", request);
            variables.put("dashboardUrl", emailConfig.getFrontendUrl() + "/organizer/dashboard");
            variables.put("createEventUrl", emailConfig.getFrontendUrl() + "/organizer/events/create");

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.ORGANIZER_REQUEST_APPROVED)
                    .to(user.getEmail())
                    .subject("Chúc mừng! Yêu cầu đăng ký Organizer đã được duyệt - TicketGo")
                    .templateName("email/organizer-request-approved")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish organizer request approved email event for: {}", user.getEmail(), e);
        }
    }

    public void sendOrganizerRequestRejectedEmail(User user, OrganizerRequest request) {
        log.info("Publishing organizer request rejected email event for: {}", user.getEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("user", user);
            variables.put("request", request);
            variables.put("rejectionReason", request.getRejectionReason());
            variables.put("resubmitUrl", emailConfig.getFrontendUrl() + "/become-organizer");

            EmailEvent event = EmailEvent.builder()
                    .type(EmailType.ORGANIZER_REQUEST_REJECTED)
                    .to(user.getEmail())
                    .subject("Thông báo về yêu cầu đăng ký Organizer - TicketGo")
                    .templateName("email/organizer-request-rejected")
                    .templateVariables(variables)
                    .build();

            emailProducer.sendEmailEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish organizer request rejected email event for: {}", user.getEmail(), e);
        }
    }

    // ==================== HELPER METHODS ====================

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return formatter.format(amount) + " ₫";
    }
}
