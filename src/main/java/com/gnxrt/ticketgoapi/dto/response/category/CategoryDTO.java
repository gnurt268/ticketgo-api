package com.gnxrt.ticketgoapi.dto.response.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private String iconUrl;

    private Integer displayOrder;

    private Boolean isActive;

    private Long eventCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}