package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.OrganizerRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizer_requests", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "organization_name", nullable = false, length = 255)
    private String organizationName;

    @Column(name = "organization_description", columnDefinition = "TEXT")
    private String organizationDescription;

    @Column(length = 500)
    private String website;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(length = 500)
    private String address;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "organization_type", length = 50)
    private String organizationType;

    @Column(name = "business_field", length = 255)
    private String businessField;

    @Column(name = "verification_document_url", length = 500)
    private String verificationDocumentUrl;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrganizerRequestStatus status = OrganizerRequestStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}