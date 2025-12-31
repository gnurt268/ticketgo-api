package com.gnxrt.ticketgoapi.dto.request.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventApprovalRequest {

    @NotBlank(message = "Action is required (APPROVE or REJECT)")
    private String action;

    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;

    private String adminNotes;
}