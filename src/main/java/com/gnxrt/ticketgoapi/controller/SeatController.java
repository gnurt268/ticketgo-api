package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.seat.GenerateSeatsRequest;
import com.gnxrt.ticketgoapi.dto.request.seat.ReserveSeatsRequest;
import com.gnxrt.ticketgoapi.dto.response.seat.SeatDTO;
import com.gnxrt.ticketgoapi.dto.response.seat.SeatMapDTO;
import com.gnxrt.ticketgoapi.dto.response.seat.SeatReservationDTO;
import com.gnxrt.ticketgoapi.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    /**
     * GET /api/zones/{zoneId}/seats
     */
    @GetMapping("/zones/{zoneId}/seats")
    public ResponseEntity<SeatMapDTO> getSeatMap(@PathVariable Long zoneId) {
        SeatMapDTO seatMap = seatService.getSeatMap(zoneId);
        return ResponseEntity.ok(seatMap);
    }

    /**
     * GET /api/zones/{zoneId}/seats/map
     */
    @GetMapping("/zones/{zoneId}/seats/map")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SeatMapDTO> getSeatMapAuthenticated(@PathVariable Long zoneId) {
        // TODO: Get current user ID from security context
        SeatMapDTO seatMap = seatService.getSeatMap(zoneId, null);
        return ResponseEntity.ok(seatMap);
    }

    /**
     * POST /api/zones/{zoneId}/seats/reserve
     */
    @PostMapping("/zones/{zoneId}/seats/reserve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SeatReservationDTO> reserveSeats(
            @PathVariable Long zoneId,
            @Valid @RequestBody ReserveSeatsRequest request
    ) {
        SeatReservationDTO reservation = seatService.reserveSeats(zoneId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    /**
     * POST /api/zones/{zoneId}/seats/release
     */
    @PostMapping("/zones/{zoneId}/seats/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> releaseSeats(
            @PathVariable Long zoneId,
            @RequestBody List<Long> seatIds
    ) {
        seatService.releaseSeats(zoneId, seatIds);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/zones/{zoneId}/seats/release-all
     */
    @PostMapping("/zones/{zoneId}/seats/release-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> releaseAllUserReservations(@PathVariable Long zoneId) {
        seatService.releaseAllUserReservations(zoneId);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/zones/{zoneId}/seats/my-reservations
     */
    @GetMapping("/zones/{zoneId}/seats/my-reservations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SeatDTO>> getMyReservedSeats(@PathVariable Long zoneId) {
        List<SeatDTO> seats = seatService.getUserReservedSeats(zoneId);
        return ResponseEntity.ok(seats);
    }

    /**
     * POST /api/organizer/zones/{zoneId}/seats/generate
     */
    @PostMapping("/organizer/zones/{zoneId}/seats/generate")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<SeatMapDTO> generateSeats(
            @PathVariable Long zoneId,
            @Valid @RequestBody GenerateSeatsRequest request
    ) {
        SeatMapDTO seatMap = seatService.generateSeats(zoneId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(seatMap);
    }

    /**
     * DELETE /api/organizer/zones/{zoneId}/seats
     */
    @DeleteMapping("/organizer/zones/{zoneId}/seats")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> deleteAllSeats(@PathVariable Long zoneId) {
        seatService.deleteAllSeats(zoneId);
        return ResponseEntity.noContent().build();
    }
}