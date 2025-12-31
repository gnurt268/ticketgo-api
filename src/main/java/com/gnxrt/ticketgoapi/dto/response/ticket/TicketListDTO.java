package com.gnxrt.ticketgoapi.dto.response.ticket;

import com.gnxrt.ticketgoapi.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketListDTO {

    private Long id;

    private String ticketCode;

    private String qrCodeUrl;

    private Long eventId;
    private String eventTitle;
    private String eventSlug;
    private String eventPosterUrl;
    private LocalDateTime eventStartDate;
    private String eventVenue;
    private String eventCity;

    private String zoneName;
    private String zoneColorCode;

    private String seatCode;

    private String holderName;

    private TicketStatus status;
    private Boolean isCheckedIn;

    private Boolean isUpcoming;
    private Boolean isPast;
    private Boolean isValid;

    private LocalDateTime createdAt;
}