package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.ImageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FaceEmbedding Entity
 */
@Entity
@Table(name = "face_embeddings", indexes = {
        @Index(name = "idx_ticket", columnList = "ticket_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @Column(columnDefinition = "JSON", nullable = false)
    private String embedding;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", length = 20)
    @Builder.Default
    private ImageType imageType = ImageType.NORMAL;

    @Column(name = "face_quality_score", precision = 4, scale = 2)
    private BigDecimal faceQualityScore;

    @Column(name = "face_detected", nullable = false)
    @Builder.Default
    private Boolean faceDetected = true;

    @Column(name = "face_alignment_score", precision = 4, scale = 2)
    private BigDecimal faceAlignmentScore;

    @Column(name = "model_name", length = 100)
    @Builder.Default
    private String modelName = "InsightFace-ArcFace";

    @Column(name = "model_version", length = 50)
    @Builder.Default
    private String modelVersion = "1.0";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}