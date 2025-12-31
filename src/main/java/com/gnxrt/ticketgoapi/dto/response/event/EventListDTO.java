package com.gnxrt.ticketgoapi.dto.response.event;

import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventListDTO {

    private Long id;

    private String title;

    private String slug;

    private String posterUrl;

    private String location;

    private String venue;

    private String city;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private EventStatus status;

    private EventType eventType;

    private Boolean isFeatured;

    private Integer viewCount;

    private Integer totalTicketsSold;

    private Long organizerId;
    private String organizerName;
    private String organizerEmail;

    private Long categoryId;
    private String categoryName;

    private LocalDateTime createdAt;
}