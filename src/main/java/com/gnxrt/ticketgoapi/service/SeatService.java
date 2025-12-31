package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.seat.GenerateSeatsRequest;
import com.gnxrt.ticketgoapi.dto.request.seat.ReserveSeatsRequest;
import com.gnxrt.ticketgoapi.dto.response.seat.SeatDTO;
import com.gnxrt.ticketgoapi.dto.response.seat.SeatMapDTO;
import com.gnxrt.ticketgoapi.dto.response.seat.SeatReservationDTO;
import com.gnxrt.ticketgoapi.enums.SeatStatus;
import com.gnxrt.ticketgoapi.enums.SeatType;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ConflictException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.Seat;
import com.gnxrt.ticketgoapi.model.TicketZone;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.SeatRepository;
import com.gnxrt.ticketgoapi.repository.TicketZoneRepository;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final TicketZoneRepository ticketZoneRepository;
    private final UserRepository userRepository;

    private static final int DEFAULT_RESERVATION_MINUTES = 15;

    public SeatMapDTO getSeatMap(Long zoneId) {
        return getSeatMap(zoneId, null);
    }

    public SeatMapDTO getSeatMap(Long zoneId, Long userId) {
        log.info("Getting seat map for zone: {}", zoneId);

        TicketZone zone = ticketZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", zoneId));

        List<Seat> seats = seatRepository.findByTicketZoneIdOrderByRowLabelAscSeatNumberAsc(zoneId);

        Map<String, List<SeatDTO>> seatsByRow = seats.stream()
                .map(seat -> mapToDTO(seat, userId))
                .collect(Collectors.groupingBy(
                        SeatDTO::getRowLabel,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        long availableCount = seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count();
        long reservedCount = seats.stream().filter(s -> s.getStatus() == SeatStatus.RESERVED).count();
        long soldCount = seats.stream().filter(s -> s.getStatus() == SeatStatus.SOLD).count();
        long blockedCount = seats.stream().filter(s -> s.getStatus() == SeatStatus.BLOCKED).count();

        List<String> rows = seatRepository.findDistinctRowsByZoneId(zoneId);
        Integer maxSeatsPerRow = seatRepository.findMaxSeatNumberByZoneId(zoneId);

        List<SeatMapDTO.SeatLegend> legend = Arrays.asList(
                SeatMapDTO.SeatLegend.builder().status("AVAILABLE").label("Còn trống").color("#22c55e").build(),
                SeatMapDTO.SeatLegend.builder().status("RESERVED").label("Đang giữ").color("#eab308").build(),
                SeatMapDTO.SeatLegend.builder().status("SOLD").label("Đã bán").color("#6b7280").build(),
                SeatMapDTO.SeatLegend.builder().status("BLOCKED").label("Không khả dụng").color("#ef4444").build(),
                SeatMapDTO.SeatLegend.builder().status("SELECTED").label("Đang chọn").color("#3b82f6").build()
        );

        return SeatMapDTO.builder()
                .eventId(zone.getEvent().getId())
                .eventTitle(zone.getEvent().getTitle())
                .ticketZoneId(zone.getId())
                .zoneName(zone.getZoneName())
                .zoneCode(zone.getZoneCode())
                .zonePrice(zone.getPrice())
                .totalRows(rows.size())
                .maxSeatsPerRow(maxSeatsPerRow != null ? maxSeatsPerRow : 0)
                .totalSeats(seats.size())
                .availableSeats((int) availableCount)
                .reservedSeats((int) reservedCount)
                .soldSeats((int) soldCount)
                .blockedSeats((int) blockedCount)
                .seatsByRow(seatsByRow)
                .allSeats(seats.stream().map(s -> mapToDTO(s, userId)).collect(Collectors.toList()))
                .legend(legend)
                .build();
    }

    @Transactional
    public SeatMapDTO generateSeats(Long zoneId, GenerateSeatsRequest request) {
        log.info("Generating seats for zone: {}", zoneId);

        TicketZone zone = ticketZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", zoneId));

        Long existingSeats = seatRepository.countByTicketZoneId(zoneId);
        if (existingSeats > 0) {
            throw new ConflictException("Khu vực đã có ghế. Hãy xóa ghế hiện tại trước.");
        }

        if (!zone.getEvent().getEnableSeatSelection()) {
            throw new BadRequestException("Sự kiện không bật chế độ chọn ghế");
        }

        List<Seat> seats = new ArrayList<>();
        Set<String> vipRowSet = request.getVipRows() != null
                ? new HashSet<>(request.getVipRows())
                : Collections.emptySet();
        Set<Integer> aisleSet = request.getAislePositions() != null
                ? new HashSet<>(request.getAislePositions())
                : Collections.emptySet();
        Set<String> blockedSet = request.getBlockedSeats() != null
                ? new HashSet<>(request.getBlockedSeats())
                : Collections.emptySet();

        BigDecimal vipMultiplier = request.getVipPriceMultiplier() != null
                ? request.getVipPriceMultiplier()
                : new BigDecimal("1.5");

        for (int row = 1; row <= request.getRows(); row++) {
            String rowLabel = generateRowLabel(row);

            for (int seatNum = 1; seatNum <= request.getSeatsPerRow(); seatNum++) {
                if (aisleSet.contains(seatNum)) {
                    continue;
                }

                String seatCode = rowLabel + seatNum;

                SeatType seatType = vipRowSet.contains(rowLabel) ? SeatType.VIP : request.getDefaultSeatType();
                BigDecimal price = seatType == SeatType.VIP
                        ? request.getBasePrice().multiply(vipMultiplier)
                        : request.getBasePrice();

                SeatStatus status = blockedSet.contains(seatCode) ? SeatStatus.BLOCKED : SeatStatus.AVAILABLE;
                if (status == SeatStatus.BLOCKED) {
                    seatType = SeatType.BLOCKED;
                }

                Seat seat = Seat.builder()
                        .ticketZone(zone)
                        .rowLabel(rowLabel)
                        .seatNumber(seatNum)
                        .seatCode(seatCode)
                        .positionX(seatNum - 1)
                        .positionY(row - 1)
                        .status(status)
                        .price(price)
                        .seatType(seatType)
                        .build();

                seats.add(seat);
            }
        }

        seatRepository.saveAll(seats);

        long availableSeats = seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count();
        zone.setTotalCapacity(seats.size());
        zone.setAvailableCapacity((int) availableSeats);
        ticketZoneRepository.save(zone);

        log.info("Generated {} seats for zone: {}", seats.size(), zoneId);

        return getSeatMap(zoneId);
    }

    @Transactional
    public SeatReservationDTO reserveSeats(Long zoneId, ReserveSeatsRequest request) {
        log.info("Reserving {} seats for zone: {}", request.getSeatIds().size(), zoneId);

        User currentUser = getCurrentUser();
        TicketZone zone = ticketZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", zoneId));

        seatRepository.releaseUserReservations(zoneId, currentUser.getId());

        List<Seat> seats = seatRepository.findByIdIn(request.getSeatIds());

        if (seats.size() != request.getSeatIds().size()) {
            throw new ResourceNotFoundException("Một số ghế không tìm thấy");
        }

        for (Seat seat : seats) {
            if (!seat.getTicketZone().getId().equals(zoneId)) {
                throw new BadRequestException("Ghế " + seat.getSeatCode() + " không thuộc khu vực này");
            }
        }

        List<Seat> unavailableSeats = seats.stream()
                .filter(s -> s.getStatus() != SeatStatus.AVAILABLE)
                .collect(Collectors.toList());

        if (!unavailableSeats.isEmpty()) {
            String unavailableCodes = unavailableSeats.stream()
                    .map(Seat::getSeatCode)
                    .collect(Collectors.joining(", "));
            throw new ConflictException("Ghế không khả dụng: " + unavailableCodes);
        }

        if (seats.size() > zone.getEvent().getMaxTicketsPerOrder()) {
            throw new BadRequestException("Không thể giữ quá " + zone.getEvent().getMaxTicketsPerOrder() + " ghế");
        }

        int minutes = request.getReservationMinutes() != null
                ? request.getReservationMinutes()
                : DEFAULT_RESERVATION_MINUTES;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(minutes);

        for (Seat seat : seats) {
            seat.reserve(currentUser, minutes);
        }
        seatRepository.saveAll(seats);

        zone.setAvailableCapacity(zone.getAvailableCapacity() - seats.size());
        zone.setReservedCapacity(zone.getReservedCapacity() + seats.size());
        ticketZoneRepository.save(zone);

        BigDecimal totalPrice = seats.stream()
                .map(Seat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Reserved {} seats for user: {}", seats.size(), currentUser.getEmail());

        return SeatReservationDTO.builder()
                .reservationId(UUID.randomUUID().toString())
                .eventId(zone.getEvent().getId())
                .eventTitle(zone.getEvent().getTitle())
                .ticketZoneId(zoneId)
                .zoneName(zone.getZoneName())
                .reservedSeats(seats.stream().map(s -> mapToDTO(s, currentUser.getId())).collect(Collectors.toList()))
                .totalSeats(seats.size())
                .totalPrice(totalPrice)
                .currency(zone.getCurrency())
                .reservedAt(now)
                .expiresAt(expiresAt)
                .remainingSeconds((int) ChronoUnit.SECONDS.between(now, expiresAt))
                .status("RESERVED")
                .message("Ghế đã được giữ trong " + minutes + " phút")
                .build();
    }

    @Transactional
    public void releaseSeats(Long zoneId, List<Long> seatIds) {
        log.info("Releasing {} seats for zone: {}", seatIds.size(), zoneId);

        User currentUser = getCurrentUser();
        List<Seat> seats = seatRepository.findByIdIn(seatIds);

        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.RESERVED &&
                    seat.getReservedBy() != null &&
                    seat.getReservedBy().getId().equals(currentUser.getId())) {
                seat.releaseReservation();
            }
        }

        seatRepository.saveAll(seats);

        TicketZone zone = ticketZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", zoneId));
        int releasedCount = (int) seats.stream()
                .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                .count();
        zone.setAvailableCapacity(zone.getAvailableCapacity() + releasedCount);
        zone.setReservedCapacity(zone.getReservedCapacity() - releasedCount);
        ticketZoneRepository.save(zone);

        log.info("Released {} seats", releasedCount);
    }

    @Transactional
    public void releaseAllUserReservations(Long zoneId) {
        User currentUser = getCurrentUser();
        log.info("Releasing all reservations for user: {} in zone: {}", currentUser.getEmail(), zoneId);

        List<Seat> userSeats = seatRepository.findByTicketZoneIdAndReservedById(zoneId, currentUser.getId());
        int releasedCount = userSeats.size();

        for (Seat seat : userSeats) {
            seat.releaseReservation();
        }
        seatRepository.saveAll(userSeats);

        if (releasedCount > 0) {
            TicketZone zone = ticketZoneRepository.findById(zoneId)
                    .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", zoneId));
            zone.setAvailableCapacity(zone.getAvailableCapacity() + releasedCount);
            zone.setReservedCapacity(zone.getReservedCapacity() - releasedCount);
            ticketZoneRepository.save(zone);
        }

        log.info("Released {} reservations for user", releasedCount);
    }

    public List<SeatDTO> getUserReservedSeats(Long zoneId) {
        User currentUser = getCurrentUser();
        List<Seat> seats = seatRepository.findByTicketZoneIdAndReservedById(zoneId, currentUser.getId());
        return seats.stream()
                .map(s -> mapToDTO(s, currentUser.getId()))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> expiredSeats = seatRepository.findExpiredReservations(now);

        if (!expiredSeats.isEmpty()) {
            log.info("Releasing {} expired reservations", expiredSeats.size());

            Map<Long, List<Seat>> seatsByZone = expiredSeats.stream()
                    .collect(Collectors.groupingBy(s -> s.getTicketZone().getId()));

            for (Map.Entry<Long, List<Seat>> entry : seatsByZone.entrySet()) {
                Long zoneId = entry.getKey();
                int count = entry.getValue().size();

                TicketZone zone = ticketZoneRepository.findById(zoneId).orElse(null);
                if (zone != null) {
                    zone.setAvailableCapacity(zone.getAvailableCapacity() + count);
                    zone.setReservedCapacity(zone.getReservedCapacity() - count);
                    ticketZoneRepository.save(zone);
                }
            }

            seatRepository.releaseExpiredReservations(now);
            log.info("Released {} expired reservations", expiredSeats.size());
        }
    }

    @Transactional
    public void deleteAllSeats(Long zoneId) {
        log.info("Deleting all seats for zone: {}", zoneId);

        Long soldCount = seatRepository.countByTicketZoneIdAndStatus(zoneId, SeatStatus.SOLD);
        if (soldCount > 0) {
            throw new BadRequestException("Không thể xóa ghế. " + soldCount + " ghế đã được bán.");
        }

        seatRepository.deleteByTicketZoneId(zoneId);

        TicketZone zone = ticketZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", zoneId));
        zone.setTotalCapacity(0);
        zone.setAvailableCapacity(0);
        zone.setReservedCapacity(0);
        ticketZoneRepository.save(zone);

        log.info("Deleted all seats for zone: {}", zoneId);
    }

    private String generateRowLabel(int row) {
        if (row <= 26) {
            return String.valueOf((char) ('A' + row - 1));
        } else {
            int first = (row - 1) / 26;
            int second = (row - 1) % 26;
            return String.valueOf((char) ('A' + first - 1)) + (char) ('A' + second);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private SeatDTO mapToDTO(Seat seat, Long currentUserId) {
        boolean isReservedByCurrentUser = seat.getReservedBy() != null &&
                currentUserId != null &&
                seat.getReservedBy().getId().equals(currentUserId);

        return SeatDTO.builder()
                .id(seat.getId())
                .ticketZoneId(seat.getTicketZone().getId())
                .zoneName(seat.getTicketZone().getZoneName())
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .seatCode(seat.getSeatCode())
                .positionX(seat.getPositionX())
                .positionY(seat.getPositionY())
                .status(seat.getStatus())
                .price(seat.getPrice())
                .seatType(seat.getSeatType())
                .isAvailable(seat.getStatus() == SeatStatus.AVAILABLE)
                .isReservedByCurrentUser(isReservedByCurrentUser)
                .reservedUntil(isReservedByCurrentUser ? seat.getReservedUntil() : null)
                .createdAt(seat.getCreatedAt())
                .updatedAt(seat.getUpdatedAt())
                .build();
    }
}