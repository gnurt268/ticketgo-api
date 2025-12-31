package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.ticketzone.TicketZoneRequest;
import com.gnxrt.ticketgoapi.dto.response.ticketzone.TicketZoneDTO;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ConflictException;
import com.gnxrt.ticketgoapi.exception.ForbiddenException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.TicketZone;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.EventRepository;
import com.gnxrt.ticketgoapi.repository.SeatRepository;
import com.gnxrt.ticketgoapi.repository.TicketZoneRepository;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketZoneService {

    private final TicketZoneRepository ticketZoneRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;

    public List<TicketZoneDTO> getZonesByEventId(Long eventId) {
        log.info("Getting ticket zones for event: {}", eventId);

        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", "id", eventId);
        }

        return ticketZoneRepository.findByEventIdOrderByDisplayOrderAsc(eventId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<TicketZoneDTO> getActiveZonesByEventId(Long eventId) {
        log.info("Getting active ticket zones for event: {}", eventId);

        return ticketZoneRepository.findByEventIdAndIsActiveTrueOrderByDisplayOrderAsc(eventId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public TicketZoneDTO getZoneById(Long zoneId) {
        log.info("Getting ticket zone: {}", zoneId);

        TicketZone zone = ticketZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", zoneId));

        return mapToDTO(zone);
    }

    @Transactional
    public TicketZoneDTO createZone(Long eventId, TicketZoneRequest request) {
        log.info("Creating ticket zone for event: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        validateEventOwnership(event);

        if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.APPROVED) {
            throw new BadRequestException("Không thể thêm khu vực vé cho sự kiện ở trạng thái: " + event.getStatus());
        }

        if (ticketZoneRepository.existsByEventIdAndZoneCode(eventId, request.getZoneCode())) {
            throw new ConflictException("Mã khu vực đã tồn tại: " + request.getZoneCode());
        }

        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null) {
            Long count = ticketZoneRepository.countByEventId(eventId);
            displayOrder = count.intValue();
        }

        TicketZone zone = TicketZone.builder()
                .event(event)
                .zoneName(request.getZoneName())
                .zoneCode(request.getZoneCode())
                .description(request.getDescription())
                .colorCode(request.getColorCode())
                .price(request.getPrice())
                .currency(request.getCurrency())
                .totalCapacity(request.getTotalCapacity())
                .availableCapacity(request.getTotalCapacity())
                .reservedCapacity(0)
                .zoneType(request.getZoneType())
                .isActive(request.getIsActive())
                .displayOrder(displayOrder)
                .saleStartDate(request.getSaleStartDate())
                .saleEndDate(request.getSaleEndDate())
                .build();

        zone = ticketZoneRepository.save(zone);
        log.info("Ticket zone created with id: {}", zone.getId());

        return mapToDTO(zone);
    }

    @Transactional
    public TicketZoneDTO updateZone(Long zoneId, TicketZoneRequest request) {
        log.info("Updating ticket zone: {}", zoneId);

        TicketZone zone = ticketZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", zoneId));

        validateEventOwnership(zone.getEvent());

        if (!zone.getZoneCode().equals(request.getZoneCode()) &&
                ticketZoneRepository.existsByEventIdAndZoneCode(zone.getEvent().getId(), request.getZoneCode())) {
            throw new ConflictException("Mã khu vực đã tồn tại: " + request.getZoneCode());
        }

        int soldCapacity = zone.getTotalCapacity() - zone.getAvailableCapacity() - zone.getReservedCapacity();

        if (request.getTotalCapacity() < soldCapacity) {
            throw new BadRequestException("Không thể giảm sức chứa dưới số lượng đã bán: " + soldCapacity);
        }

        int newAvailableCapacity = request.getTotalCapacity() - soldCapacity - zone.getReservedCapacity();

        zone.setZoneName(request.getZoneName());
        zone.setZoneCode(request.getZoneCode());
        zone.setDescription(request.getDescription());
        zone.setColorCode(request.getColorCode());
        zone.setPrice(request.getPrice());
        zone.setCurrency(request.getCurrency());
        zone.setTotalCapacity(request.getTotalCapacity());
        zone.setAvailableCapacity(newAvailableCapacity);
        zone.setZoneType(request.getZoneType());
        zone.setIsActive(request.getIsActive());
        zone.setSaleStartDate(request.getSaleStartDate());
        zone.setSaleEndDate(request.getSaleEndDate());

        if (request.getDisplayOrder() != null) {
            zone.setDisplayOrder(request.getDisplayOrder());
        }

        zone = ticketZoneRepository.save(zone);
        log.info("Ticket zone updated: {}", zone.getId());

        return mapToDTO(zone);
    }

    @Transactional
    public void deleteZone(Long zoneId) {
        log.info("Deleting ticket zone: {}", zoneId);

        TicketZone zone = ticketZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", zoneId));

        validateEventOwnership(zone.getEvent());

        int soldCapacity = zone.getTotalCapacity() - zone.getAvailableCapacity() - zone.getReservedCapacity();
        if (soldCapacity > 0) {
            throw new BadRequestException("Không thể xóa khu vực đã bán " + soldCapacity + " vé");
        }

        seatRepository.deleteByTicketZoneId(zoneId);

        ticketZoneRepository.delete(zone);
        log.info("Ticket zone deleted: {}", zoneId);
    }

    @Transactional
    public TicketZoneDTO toggleActive(Long zoneId) {
        log.info("Toggling active status for zone: {}", zoneId);

        TicketZone zone = ticketZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", zoneId));

        validateEventOwnership(zone.getEvent());

        zone.setIsActive(!zone.getIsActive());
        zone = ticketZoneRepository.save(zone);

        log.info("Zone active status toggled to: {} for id: {}", zone.getIsActive(), zoneId);
        return mapToDTO(zone);
    }

    @Transactional
    public void updateDisplayOrders(Long eventId, List<Long> zoneIds) {
        log.info("Updating display orders for event: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        validateEventOwnership(event);

        for (int i = 0; i < zoneIds.size(); i++) {
            final int index = i;
            TicketZone zone = ticketZoneRepository.findById(zoneIds.get(i))
                    .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", zoneIds.get(index)));

            if (!zone.getEvent().getId().equals(eventId)) {
                throw new BadRequestException("Khu vực không thuộc sự kiện này");
            }

            zone.setDisplayOrder(i);
            ticketZoneRepository.save(zone);
        }

        log.info("Display orders updated for {} zones", zoneIds.size());
    }

    private void validateEventOwnership(Event event) {
        User currentUser = getCurrentUser();
        if (!event.getOrganizer().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền quản lý khu vực vé của sự kiện này");
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private TicketZoneDTO mapToDTO(TicketZone zone) {
        int soldCapacity = zone.getTotalCapacity() - zone.getAvailableCapacity() - zone.getReservedCapacity();
        double soldPercentage = zone.getTotalCapacity() > 0
                ? (double) soldCapacity / zone.getTotalCapacity() * 100
                : 0.0;

        return TicketZoneDTO.builder()
                .id(zone.getId())
                .eventId(zone.getEvent().getId())
                .eventTitle(zone.getEvent().getTitle())
                .zoneName(zone.getZoneName())
                .zoneCode(zone.getZoneCode())
                .description(zone.getDescription())
                .colorCode(zone.getColorCode())
                .price(zone.getPrice())
                .currency(zone.getCurrency())
                .totalCapacity(zone.getTotalCapacity())
                .availableCapacity(zone.getAvailableCapacity())
                .reservedCapacity(zone.getReservedCapacity())
                .soldCapacity(soldCapacity)
                .zoneType(zone.getZoneType())
                .isActive(zone.getIsActive())
                .displayOrder(zone.getDisplayOrder())
                .saleStartDate(zone.getSaleStartDate())
                .saleEndDate(zone.getSaleEndDate())
                .isSaleActive(zone.isSaleActive())
                .isSoldOut(zone.getAvailableCapacity() == 0)
                .soldPercentage(Math.round(soldPercentage * 100.0) / 100.0)
                .createdAt(zone.getCreatedAt())
                .updatedAt(zone.getUpdatedAt())
                .build();
    }
}