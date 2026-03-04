package com.backend.old_bicycle_project.entity;

import com.backend.old_bicycle_project.entity.enums.ConditionType;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brake_type_id", nullable = false)
    private BrakeType brakeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "frame_material_id", nullable = false)
    private FrameMaterial frameMaterial;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 15, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "frame_size")
    private String frameSize;

    @Column(name = "wheel_size")
    private String wheelSize;

    private String groupset;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "condition_type")
    @Builder.Default
    private ConditionType condition = ConditionType.used;

    private String province;
    private String district;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "product_status")
    @Builder.Default
    private ProductStatus status = ProductStatus.pending;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
