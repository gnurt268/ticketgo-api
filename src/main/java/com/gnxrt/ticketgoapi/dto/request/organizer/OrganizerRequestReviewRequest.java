package com.gnxrt.ticketgoapi.dto.request.organizer;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerRequestReviewRequest {

    @NotNull(message = "Trạng thái duyệt không được để trống")
    private Boolean approved;

    private String rejectionReason;

    private String adminNotes;
}