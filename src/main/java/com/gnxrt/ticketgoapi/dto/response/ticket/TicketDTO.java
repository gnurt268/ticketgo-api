package com.gnxrt.ticketgoapi.dto.response.ticket;

import com.gnxrt.ticketgoapi.enums.CheckInMethod;
import com.gnxrt.ticketgoapi.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {

    private Long id;

    private String ticketCode;

    private String qrCode;

    private String qrCodeUrl;

    private Long eventId;
    private String eventTitle;
    private String eventSlug;
    private String eventPosterUrl;
    private String eventBannerUrl;
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
    private String eventVenue;
    private String eventAddress;
    private String eventCity;

    private Long ticketZoneId;
    private String zoneName;
    private String zoneCode;
    private String zoneColorCode;
    private BigDecimal zonePrice;

    private Long seatId;
    private String seatCode;
    private String rowNumber;
    private String seatNumber;

    private String holderName;
    private String holderEmail;
    private String holderPhone;
    private String holderIdNumber;

    private Long orderId;
    private String orderCode;

    private TicketStatus status;
    private Boolean isCheckedIn;
    private LocalDateTime checkedInAt;
    private CheckInMethod checkedInBy;
    private BigDecimal checkedInConfidence;

    private Boolean hasFaceImage;
    private String faceImageUrl;
    private LocalDateTime faceUploadedAt;
    private Boolean requiresFaceUpload;

    private String transferredFromEmail;
    private LocalDateTime transferredAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Boolean isValid;
    private Boolean canCheckIn;
    private Boolean canTransfer;
    private String statusMessage;
}