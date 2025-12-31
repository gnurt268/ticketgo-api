package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.ticket.TransferTicketRequest;
import com.gnxrt.ticketgoapi.dto.response.ticket.TicketDTO;
import com.gnxrt.ticketgoapi.dto.response.ticket.TicketListDTO;
import com.gnxrt.ticketgoapi.enums.TicketStatus;
import com.gnxrt.ticketgoapi.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * GET /api/tickets
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TicketListDTO>> getMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TicketListDTO> tickets = ticketService.getMyTickets(pageable);
        return ResponseEntity.ok(tickets);
    }

    /**
     * GET /api/tickets/upcoming
     */
    @GetMapping("/upcoming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TicketListDTO>> getMyUpcomingTickets() {
        List<TicketListDTO> tickets = ticketService.getMyUpcomingTickets();
        return ResponseEntity.ok(tickets);
    }

    /**
     * GET /api/tickets/past
     */
    @GetMapping("/past")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TicketListDTO>> getMyPastTickets() {
        List<TicketListDTO> tickets = ticketService.getMyPastTickets();
        return ResponseEntity.ok(tickets);
    }

    /**
     * GET /api/tickets/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketDTO> getTicketDetail(@PathVariable Long id) {
        TicketDTO ticket = ticketService.getTicketDetail(id);
        return ResponseEntity.ok(ticket);
    }

    /**
     * GET /api/tickets/code/{ticketCode}
     */
    @GetMapping("/code/{ticketCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketDTO> getTicketByCode(@PathVariable String ticketCode) {
        TicketDTO ticket = ticketService.getTicketByCode(ticketCode);
        return ResponseEntity.ok(ticket);
    }

    /**
     * GET /api/tickets/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TicketDTO>> getTicketsByOrder(@PathVariable Long orderId) {
        List<TicketDTO> tickets = ticketService.getTicketsByOrder(orderId);
        return ResponseEntity.ok(tickets);
    }

    /**
     * GET /api/tickets/event/{eventId}
     */
    @GetMapping("/event/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TicketDTO>> getMyTicketsForEvent(@PathVariable Long eventId) {
        List<TicketDTO> tickets = ticketService.getMyTicketsForEvent(eventId);
        return ResponseEntity.ok(tickets);
    }

    /**
     * POST /api/tickets/{id}/transfer
     */
    @PostMapping("/{id}/transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketDTO> transferTicket(
            @PathVariable Long id,
            @Valid @RequestBody TransferTicketRequest request
    ) {
        TicketDTO ticket = ticketService.transferTicket(id, request);
        return ResponseEntity.ok(ticket);
    }

    /**
     * GET /api/tickets/{id}/qrcode
     */
    @GetMapping("/{id}/qrcode")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadQRCode(@PathVariable Long id) {
        byte[] qrCode = ticketService.downloadTicketQRCode(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("attachment", "ticket-qrcode.png");

        return ResponseEntity.ok()
                .headers(headers)
                .body(qrCode);
    }

    /**
     * GET /api/tickets/organizer/events/{eventId}
     */
    @GetMapping("/organizer/events/{eventId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Page<TicketListDTO>> getTicketsByEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TicketStatus status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<TicketListDTO> tickets;
        if (status != null) {
            tickets = ticketService.getTicketsByEventAndStatus(eventId, status, pageable);
        } else {
            tickets = ticketService.getTicketsByEvent(eventId, pageable);
        }

        return ResponseEntity.ok(tickets);
    }

    /**
     * GET /api/tickets/organizer/validate/{ticketCode}
     */
    @GetMapping("/organizer/validate/{ticketCode}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<TicketDTO> validateTicket(@PathVariable String ticketCode) {
        TicketDTO ticket = ticketService.validateTicketForCheckIn(ticketCode);
        return ResponseEntity.ok(ticket);
    }
}