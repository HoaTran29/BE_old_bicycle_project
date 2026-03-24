package com.backend.old_bicycle_project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "size_chart_rows",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_size_chart_rows_chart_frame_size", columnNames = {"size_chart_id", "frame_size"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SizeChartRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "size_chart_id", nullable = false)
    private SizeChart sizeChart;

    @Column(name = "frame_size", nullable = false)
    private String frameSize;

    @Column(name = "height_min_cm", nullable = false)
    private Integer heightMinCm;

    @Column(name = "height_max_cm", nullable = false)
    private Integer heightMaxCm;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
