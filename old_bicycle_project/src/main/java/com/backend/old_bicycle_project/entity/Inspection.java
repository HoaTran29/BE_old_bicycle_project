package com.backend.old_bicycle_project.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inspections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id")
    private User inspector;

    @Column(name = "overall_score", precision = 3, scale = 1)
    private java.math.BigDecimal overallScore;

    @Column(name = "frame_score")
    private Integer frameScore;

    @Column(name = "fork_score")
    private Integer forkScore;

    @Column(name = "brakes_score")
    private Integer brakesScore;

    @Column(name = "drivetrain_score")
    private Integer drivetrainScore;

    @Column(name = "wheels_score")
    private Integer wheelsScore;

    @Column(name = "wear_percentage")
    private Integer wearPercentage;

    @Column(name = "expert_notes", columnDefinition = "TEXT")
    private String expertNotes;

    @Builder.Default
    @Column(name = "passed")
    private Boolean passed = false;

    @Column(name = "report_file_url", columnDefinition = "TEXT")
    private String reportFileUrl;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
