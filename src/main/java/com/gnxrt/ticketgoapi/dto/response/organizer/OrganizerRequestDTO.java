package com.gnxrt.ticketgoapi.dto.response.organizer;

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
public class OrganizerRequestDTO {

    private Long id;

    // User info
    private Long userId;
    private String userEmail;
    private String userFullName;
    private String userPhone;
    private String userAvatarUrl;

    // Organization info
    private String organizationName;
    private String organizationDescription;
    private String website;
    private String contactPhone;
    private String address;
    private String taxCode;
    private String organizationType;
    private String businessField;
    private String verificationDocumentUrl;
    private String reason;

    // Status
    private String status;
    private String rejectionReason;

    // Review info
    private Long reviewedById;
    private String reviewedByName;
    private String reviewedAt;
    private String adminNotes;

    // Timestamps
    private String createdAt;
    private String updatedAt;
}