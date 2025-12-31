package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.response.checkin.CheckInResponse;
import com.gnxrt.ticketgoapi.enums.CheckInMethod;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.TicketStatus;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.Ticket;
import com.gnxrt.ticketgoapi.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final TicketRepository ticketRepository;
    private final QRCodeService qrCodeService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    /**
     * Validate QR code without check-in (preview ticket info)
     */
    public CheckInResponse validateQR(String qrContent) {
        log.info("Validating QR code");

        // 1. Validate QR signature
        QRCodeService.QRCodeValidationResult result = qrCodeService.validateQRCode(qrContent);
        if (!result.isValid()) {
            log.warn("QR validation failed: {}", result.getErrorMessage());
            return CheckInResponse.error(result.getErrorMessage());
        }

        // 2. Find ticket
        Ticket ticket = ticketRepository.findByTicketCode(result.getTicketCode()).orElse(null);
        if (ticket == null) {
            return CheckInResponse.error("Không tìm thấy vé với mã: " + result.getTicketCode());
        }

        // 3. Verify QR secret
        if (!ticket.getQrCode().equals(result.getQrCode())) {
            log.warn("QR secret mismatch for ticket: {}", result.getTicketCode());
            return CheckInResponse.error("Mã QR không hợp lệ hoặc đã bị thay đổi");
        }

        return buildResponse(ticket, false, "Vé hợp lệ - Sẵn sàng check-in");
    }

    /**
     * Check-in ticket by QR code
     */
    @Transactional
    public CheckInResponse checkIn(String qrContent, String staffEmail) {
        log.info("Processing check-in, staff: {}", staffEmail);

        // 1. Validate QR signature
        QRCodeService.QRCodeValidationResult result = qrCodeService.validateQRCode(qrContent);
        if (!result.isValid()) {
            log.warn("QR validation failed: {}", result.getErrorMessage());
            return CheckInResponse.error(result.getErrorMessage());
        }

        // 2. Find ticket
        Ticket ticket = ticketRepository.findByTicketCode(result.getTicketCode()).orElse(null);
        if (ticket == null) {
            return CheckInResponse.error("Không tìm thấy vé với mã: " + result.getTicketCode());
        }

        // 3. Verify QR secret
        if (!ticket.getQrCode().equals(result.getQrCode())) {
            log.warn("QR secret mismatch for ticket: {}", result.getTicketCode());
            return CheckInResponse.error("Mã QR không hợp lệ hoặc đã bị thay đổi");
        }

        // 4. Check ticket status
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            String statusMsg = switch (ticket.getStatus()) {
                case CANCELLED -> "Vé đã bị hủy";
                case REFUNDED -> "Vé đã được hoàn tiền";
                case PENDING -> "Vé chưa được thanh toán";
                case TRANSFERRED -> "Vé đã được chuyển nhượng";
                case USED -> "Vé đã được sử dụng";
                default -> "Vé không hợp lệ: " + ticket.getStatus().name();
            };
            return buildResponse(ticket, false, statusMsg);
        }

        // 5. Check if already checked in
        if (ticket.getIsCheckedIn()) {
            String methodName = ticket.getCheckedInBy() != null
                    ? ticket.getCheckedInBy().name()
                    : "N/A";
            String msg = String.format("Vé đã được check-in lúc %s (phương thức: %s)",
                    ticket.getCheckedInAt().format(TIME_FORMATTER),
                    methodName);
            return buildResponse(ticket, false, msg);
        }

        // 6. Check event status
        Event event = ticket.getEvent();
        if (event.getStatus() != EventStatus.PUBLISHED) {
            return buildResponse(ticket, false, "Sự kiện không hoạt động");
        }

        // 7. Check event time (allow check-in 2 hours before start)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInStart = event.getStartDate().minusHours(2);

        if (now.isBefore(checkInStart)) {
            String msg = String.format("Chưa đến thời gian check-in. Bắt đầu từ %s",
                    checkInStart.format(TIME_FORMATTER));
            return buildResponse(ticket, false, msg);
        }

        if (now.isAfter(event.getEndDate())) {
            return buildResponse(ticket, false, "Sự kiện đã kết thúc");
        }

        // 8. Perform check-in using helper method in Ticket model
        ticket.checkIn(CheckInMethod.QR_CODE, null);
        ticket = ticketRepository.save(ticket);

        log.info("Check-in successful: ticket={}, event={}, method=QR_CODE, staff={}",
                ticket.getTicketCode(), event.getTitle(), staffEmail);

        // Get stats
        Long totalCheckedIn = ticketRepository.countByEventIdAndIsCheckedInTrue(event.getId());
        Long totalActive = ticketRepository.countByEventIdAndStatus(event.getId(), TicketStatus.ACTIVE);
        Long totalUsed = ticketRepository.countByEventIdAndStatus(event.getId(), TicketStatus.USED);

        CheckInResponse response = buildResponse(ticket, true, "Check-in thành công!");
        response.setTotalCheckedIn(totalCheckedIn);
        response.setTotalTickets(totalActive + totalUsed);

        return response;
    }

    /**
     * Get check-in stats for an event
     */
    public CheckInResponse getEventStats(Long eventId) {
        Long totalCheckedIn = ticketRepository.countByEventIdAndIsCheckedInTrue(eventId);
        Long totalActive = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.ACTIVE);
        Long totalUsed = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.USED);

        return CheckInResponse.builder()
                .success(true)
                .totalCheckedIn(totalCheckedIn)
                .totalTickets(totalActive + totalUsed)
                .build();
    }

    private CheckInResponse buildResponse(Ticket ticket, boolean success, String message) {
        Event event = ticket.getEvent();

        return CheckInResponse.builder()
                .success(success)
                .message(message)
                .ticketId(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .status(ticket.getStatus().name())
                .holderName(ticket.getHolderName())
                .holderEmail(ticket.getHolderEmail())
                .holderPhone(ticket.getHolderPhone())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventStartDate(event.getStartDate())
                .eventVenue(event.getVenue())
                .zoneName(ticket.getTicketZone().getZoneName())
                .zoneColorCode(ticket.getTicketZone().getColorCode())
                .seatCode(ticket.getSeatNumber())
                .rowNumber(ticket.getRowNumber())
                .checkedInAt(ticket.getCheckedInAt())
                .checkedInMethod(ticket.getCheckedInBy() != null ? ticket.getCheckedInBy().name() : null)
                .build();
    }
}