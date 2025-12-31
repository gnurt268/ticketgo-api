package com.gnxrt.ticketgoapi.dto.response.checkin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponse {

    private boolean success;
    private String message;

    // Ticket info
    private Long ticketId;
    private String ticketCode;
    private String status;

    // Holder info
    private String holderName;
    private String holderEmail;
    private String holderPhone;

    // Event info
    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventStartDate;
    private String eventVenue;

    // Zone & Seat
    private String zoneName;
    private String zoneColorCode;
    private String seatCode;
    private String rowNumber;

    // Check-in info
    private LocalDateTime checkedInAt;
    private String checkedInMethod;

    // Stats
    private Long totalCheckedIn;
    private Long totalTickets;

    public static CheckInResponse success(String message) {
        return CheckInResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static CheckInResponse error(String message) {
        return CheckInResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}