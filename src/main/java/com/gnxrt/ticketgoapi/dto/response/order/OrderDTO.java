package com.gnxrt.ticketgoapi.dto.response.order;

import com.gnxrt.ticketgoapi.enums.PaymentMethod;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long id;

    private String orderCode;

    private Long eventId;
    private String eventTitle;
    private String eventSlug;
    private String eventPosterUrl;
    private LocalDateTime eventStartDate;
    private String eventVenue;
    private String eventAddress;

    private Long ticketZoneId;
    private String zoneName;
    private String zoneCode;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String currency;

    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String paymentTransactionId;
    private LocalDateTime paidAt;

    private String buyerName;
    private String buyerEmail;
    private String buyerPhone;

    private Long userId;
    private String userName;

    private String notes;

    private List<TicketSummary> tickets;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String paymentUrl;

    private LocalDateTime paymentExpiredAt;
    private Integer remainingSeconds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketSummary {
        private Long id;
        private String ticketCode;
        private String holderName;
        private String holderEmail;
        private String seatCode;
        private String rowNumber;
        private String status;
        private Boolean isCheckedIn;
    }
}