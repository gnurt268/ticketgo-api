package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.response.checkin.CheckInResponse;
import com.gnxrt.ticketgoapi.dto.response.checkin.CheckinEventDTO;
import com.gnxrt.ticketgoapi.enums.CheckInMethod;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.Role;
import com.gnxrt.ticketgoapi.enums.TicketStatus;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.CheckIn;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.EventStaff;
import com.gnxrt.ticketgoapi.model.Ticket;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.CheckInRepository;
import com.gnxrt.ticketgoapi.repository.EventRepository;
import com.gnxrt.ticketgoapi.repository.EventStaffRepository;
import com.gnxrt.ticketgoapi.repository.TicketRepository;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final TicketRepository ticketRepository;
    private final QRCodeService qrCodeService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventStaffRepository eventStaffRepository;
    private final CheckInRepository checkInRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // ============================================================
    // Phạm vi: ai được check-in sự kiện nào
    // ============================================================

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Quyền check-in 1 sự kiện = ADMIN ∨ chủ sự kiện ∨ nhân viên được giao (EventStaff).
     */
    private void assertCanCheckIn(User user, Long eventId) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (eventRepository.existsByIdAndOrganizerId(eventId, user.getId())) {
            return;
        }
        if (eventStaffRepository.existsByEventIdAndStaffId(eventId, user.getId())) {
            return;
        }
        log.warn("User {} không có quyền check-in sự kiện {}", user.getEmail(), eventId);
        throw new AccessDeniedException("Bạn không có quyền soát vé cho sự kiện này");
    }

    /**
     * Danh sách sự kiện (PUBLISHED) mà người dùng được phép check-in.
     */
    public List<CheckinEventDTO> listCheckinableEvents(String userEmail) {
        User user = resolveUser(userEmail);
        Set<Event> events = new LinkedHashSet<>();

        if (user.getRole() == Role.ADMIN) {
            events.addAll(eventRepository.findByStatusOrderByStartDateAsc(EventStatus.PUBLISHED));
        } else {
            if (user.getRole() == Role.ORGANIZER) {
                events.addAll(eventRepository.findByOrganizerIdAndStatusIn(
                        user.getId(), List.of(EventStatus.PUBLISHED)));
            }
            for (EventStaff es : eventStaffRepository.findByStaffId(user.getId())) {
                Event e = es.getEvent();
                if (e.getStatus() == EventStatus.PUBLISHED) {
                    events.add(e);
                }
            }
        }

        return events.stream().map(this::toCheckinEventDTO).collect(Collectors.toList());
    }

    private CheckinEventDTO toCheckinEventDTO(Event event) {
        Long totalCheckedIn = ticketRepository.countByEventIdAndIsCheckedInTrue(event.getId());
        Long totalActive = ticketRepository.countByEventIdAndStatus(event.getId(), TicketStatus.ACTIVE);
        Long totalUsed = ticketRepository.countByEventIdAndStatus(event.getId(), TicketStatus.USED);
        return CheckinEventDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .posterUrl(event.getPosterUrl())
                .venue(event.getVenue())
                .city(event.getCity())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .totalCheckedIn(totalCheckedIn)
                .totalTickets(totalActive + totalUsed)
                .build();
    }

    // ============================================================
    // Validate / Check-in theo sự kiện
    // ============================================================

    /**
     * Xem trước vé (không check-in).
     */
    public CheckInResponse validateQR(Long eventId, String qrContent, String userEmail) {
        User user = resolveUser(userEmail);
        assertCanCheckIn(user, eventId);

        QRCodeService.QRCodeValidationResult result = qrCodeService.validateQRCode(qrContent);
        if (!result.isValid()) {
            return CheckInResponse.error(result.getErrorMessage());
        }

        Ticket ticket = ticketRepository.findByTicketCode(result.getTicketCode()).orElse(null);
        if (ticket == null) {
            return CheckInResponse.error("Không tìm thấy vé với mã: " + result.getTicketCode());
        }
        if (!ticket.getQrCode().equals(result.getQrCode())) {
            return CheckInResponse.error("Mã QR không hợp lệ hoặc đã bị thay đổi");
        }
        if (!ticket.getEvent().getId().equals(eventId)) {
            return buildResponse(ticket, false, "Vé không thuộc sự kiện này");
        }

        return buildResponse(ticket, false, "Vé hợp lệ - Sẵn sàng check-in");
    }

    /**
     * Check-in vé theo sự kiện (ghi audit vào check_ins).
     */
    @Transactional
    public CheckInResponse checkIn(Long eventId, String qrContent, String userEmail,
                                   String deviceInfo, String ipAddress) {
        User user = resolveUser(userEmail);
        assertCanCheckIn(user, eventId);

        // 1. Validate QR
        QRCodeService.QRCodeValidationResult result = qrCodeService.validateQRCode(qrContent);
        if (!result.isValid()) {
            return CheckInResponse.error(result.getErrorMessage());
        }

        // 2. Tìm vé
        Ticket ticket = ticketRepository.findByTicketCode(result.getTicketCode()).orElse(null);
        if (ticket == null) {
            return CheckInResponse.error("Không tìm thấy vé với mã: " + result.getTicketCode());
        }

        // 3. Verify QR secret
        if (!ticket.getQrCode().equals(result.getQrCode())) {
            return CheckInResponse.error("Mã QR không hợp lệ hoặc đã bị thay đổi");
        }

        // 4. Chặn vé sai sự kiện
        if (!ticket.getEvent().getId().equals(eventId)) {
            return buildResponse(ticket, false, "Vé không thuộc sự kiện này");
        }

        // 5. Trạng thái vé
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            String statusMsg = switch (ticket.getStatus()) {
                case CANCELLED -> "Vé đã bị hủy";
                case REFUNDED -> "Vé đã được hoàn tiền";
                case PENDING -> "Vé chưa được thanh toán";
                case TRANSFERRED -> "Vé đã được chuyển nhượng";
                case USED -> "Vé đã được sử dụng";
                default -> "Vé không hợp lệ: " + ticket.getStatus().name();
            };
            saveAudit(ticket, user, deviceInfo, ipAddress, false, statusMsg);
            return buildResponse(ticket, false, statusMsg);
        }

        // 6. Đã check-in?
        if (Boolean.TRUE.equals(ticket.getIsCheckedIn())) {
            String methodName = ticket.getCheckedInBy() != null ? ticket.getCheckedInBy().name() : "N/A";
            String msg = String.format("Vé đã được check-in lúc %s (phương thức: %s)",
                    ticket.getCheckedInAt().format(TIME_FORMATTER), methodName);
            saveAudit(ticket, user, deviceInfo, ipAddress, false, msg);
            return buildResponse(ticket, false, msg);
        }

        // 7. Trạng thái sự kiện
        Event event = ticket.getEvent();
        if (event.getStatus() != EventStatus.PUBLISHED) {
            String msg = "Sự kiện không hoạt động";
            saveAudit(ticket, user, deviceInfo, ipAddress, false, msg);
            return buildResponse(ticket, false, msg);
        }

        // 8. Khung thời gian (cho phép check-in từ 2h trước giờ bắt đầu)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInStart = event.getStartDate().minusHours(2);
        if (now.isBefore(checkInStart)) {
            String msg = String.format("Chưa đến thời gian check-in. Bắt đầu từ %s",
                    checkInStart.format(TIME_FORMATTER));
            saveAudit(ticket, user, deviceInfo, ipAddress, false, msg);
            return buildResponse(ticket, false, msg);
        }
        if (now.isAfter(event.getEndDate())) {
            String msg = "Sự kiện đã kết thúc";
            saveAudit(ticket, user, deviceInfo, ipAddress, false, msg);
            return buildResponse(ticket, false, msg);
        }

        // 9. Check-in
        ticket.checkIn(CheckInMethod.QR_CODE, null);
        ticket = ticketRepository.save(ticket);
        saveAudit(ticket, user, deviceInfo, ipAddress, true, null);

        log.info("Check-in OK: ticket={}, event={}, staff={}",
                ticket.getTicketCode(), event.getTitle(), user.getEmail());

        Long totalCheckedIn = ticketRepository.countByEventIdAndIsCheckedInTrue(event.getId());
        Long totalActive = ticketRepository.countByEventIdAndStatus(event.getId(), TicketStatus.ACTIVE);
        Long totalUsed = ticketRepository.countByEventIdAndStatus(event.getId(), TicketStatus.USED);

        CheckInResponse response = buildResponse(ticket, true, "Check-in thành công!");
        response.setTotalCheckedIn(totalCheckedIn);
        response.setTotalTickets(totalActive + totalUsed);
        return response;
    }

    /**
     * Thống kê check-in của sự kiện.
     */
    public CheckInResponse getEventStats(Long eventId, String userEmail) {
        User user = resolveUser(userEmail);
        assertCanCheckIn(user, eventId);

        Long totalCheckedIn = ticketRepository.countByEventIdAndIsCheckedInTrue(eventId);
        Long totalActive = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.ACTIVE);
        Long totalUsed = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.USED);

        return CheckInResponse.builder()
                .success(true)
                .totalCheckedIn(totalCheckedIn)
                .totalTickets(totalActive + totalUsed)
                .build();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private void saveAudit(Ticket ticket, User staff, String deviceInfo, String ipAddress,
                           boolean success, String failureReason) {
        CheckIn record = CheckIn.builder()
                .ticket(ticket)
                .event(ticket.getEvent())
                .checkInMethod(CheckInMethod.QR_CODE)
                .staff(staff)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .isSuccessful(success)
                .failureReason(success ? null : failureReason)
                .build();
        checkInRepository.save(record);
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
