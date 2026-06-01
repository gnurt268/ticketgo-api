package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.response.eventstaff.EventStaffDTO;
import com.gnxrt.ticketgoapi.enums.Role;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ConflictException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.EventStaff;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.EventRepository;
import com.gnxrt.ticketgoapi.repository.EventStaffRepository;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Quản lý phân công nhân viên check-in cho sự kiện (organizer chủ sự kiện).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventStaffService {

    private final EventStaffRepository eventStaffRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public List<EventStaffDTO> getStaff(Long eventId) {
        Event event = requireOwnedEvent(eventId);
        return eventStaffRepository.findByEventId(event.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventStaffDTO assignStaff(Long eventId, String email) {
        Event event = requireOwnedEvent(eventId);

        User staff = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (staff.getRole() != Role.STAFF) {
            throw new BadRequestException(
                    "Người dùng phải có vai trò STAFF mới được giao soát vé. Vui lòng liên hệ ADMIN cấp quyền STAFF.");
        }

        if (eventStaffRepository.existsByEventIdAndStaffId(event.getId(), staff.getId())) {
            throw new ConflictException("Nhân viên đã được giao cho sự kiện này");
        }

        EventStaff assignment = EventStaff.builder()
                .event(event)
                .staff(staff)
                .assignedBy(getCurrentUser())
                .build();
        assignment = eventStaffRepository.save(assignment);
        log.info("Assigned staff {} to event {}", staff.getEmail(), event.getId());

        return mapToDTO(assignment);
    }

    @Transactional
    public void removeStaff(Long eventId, Long userId) {
        requireOwnedEvent(eventId);
        eventStaffRepository.deleteByEventIdAndStaffId(eventId, userId);
        log.info("Removed staff {} from event {}", userId, eventId);
    }

    // ============================================================
    // Helpers
    // ============================================================

    /**
     * Lấy sự kiện và đảm bảo current-user là chủ sự kiện (chống đụng sự kiện người khác).
     */
    private Event requireOwnedEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));
        User current = getCurrentUser();
        if (!event.getOrganizer().getId().equals(current.getId())) {
            throw new AccessDeniedException("Bạn không sở hữu sự kiện này");
        }
        return event;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private EventStaffDTO mapToDTO(EventStaff es) {
        User staff = es.getStaff();
        return EventStaffDTO.builder()
                .id(es.getId())
                .userId(staff.getId())
                .email(staff.getEmail())
                .fullName(staff.getFullName())
                .assignedAt(es.getCreatedAt())
                .build();
    }
}
