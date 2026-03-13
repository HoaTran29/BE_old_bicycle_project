package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.product.ProductCreateRequest;
import com.backend.old_bicycle_project.dto.product.ProductFilterRequest;
import com.backend.old_bicycle_project.dto.product.ProductResponse;
import com.backend.old_bicycle_project.dto.product.ProductUpdateRequest;
import com.backend.old_bicycle_project.entity.Brand;
import com.backend.old_bicycle_project.entity.BrakeType;
import com.backend.old_bicycle_project.entity.Category;
import com.backend.old_bicycle_project.entity.FrameMaterial;
import com.backend.old_bicycle_project.entity.Inspection;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.ProductImage;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.BrandRepository;
import com.backend.old_bicycle_project.repository.BrakeTypeRepository;
import com.backend.old_bicycle_project.repository.CategoryRepository;
import com.backend.old_bicycle_project.repository.FrameMaterialRepository;
import com.backend.old_bicycle_project.repository.InspectionRepository;
import com.backend.old_bicycle_project.repository.ProductImageRepository;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.specification.ProductSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final List<ProductStatus> PUBLIC_VISIBLE_STATUSES = List.of(
            ProductStatus.active,
            ProductStatus.pending_inspection,
            ProductStatus.inspected_passed,
            ProductStatus.inspected_failed
    );

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final BrakeTypeRepository brakeTypeRepository;
    private final FrameMaterialRepository frameMaterialRepository;
    private final InspectionRepository inspectionRepository;
    private final StorageService storageService;

    public Page<ProductResponse> searchProducts(ProductFilterRequest filter, int page, int size) {
        Specification<Product> spec = ProductSpecification.fromFilter(filter);
        Sort sort = buildSort(filter);
        Pageable pageable = PageRequest.of(page, size, sort);

        return productRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    public ProductResponse getById(UUID id) {
        Product product = findActiveProductById(id);
        if (!PUBLIC_VISIBLE_STATUSES.contains(product.getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return toResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest request, List<MultipartFile> images, User seller) {
        validateRequiredTechnicalFields(request.getFrameSize(), request.getWheelSize());
        validateMinimumImages(images);

        BrakeType brakeType = brakeTypeRepository.findById(request.getBrakeTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        FrameMaterial frameMaterial = frameMaterialRepository.findById(request.getFrameMaterialId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        Brand brand = request.getBrandId() != null
                ? brandRepository.findById(request.getBrandId()).orElse(null)
                : null;
        Category category = request.getCategoryId() != null
                ? categoryRepository.findById(request.getCategoryId()).orElse(null)
                : null;

        Product product = Product.builder()
                .seller(seller)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .brand(brand)
                .category(category)
                .brakeType(brakeType)
                .frameMaterial(frameMaterial)
                .frameSize(request.getFrameSize())
                .wheelSize(request.getWheelSize())
                .groupset(request.getGroupset())
                .condition(request.getCondition() != null ? request.getCondition() : com.backend.old_bicycle_project.entity.enums.ConditionType.used)
                .province(request.getProvince())
                .district(request.getDistrict())
                .status(ProductStatus.pending)
                .build();

        productRepository.save(product);
        product.setExpiresAt(product.getCreatedAt().plusDays(30));
        product.setImages(uploadImages(product, images));
        productRepository.save(product);

        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductUpdateRequest request, List<MultipartFile> newImages, User currentUser) {
        Product product = findActiveProductById(id);

        if (!product.getSeller().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (request.getTitle() != null) product.setTitle(request.getTitle());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getOriginalPrice() != null) product.setOriginalPrice(request.getOriginalPrice());
        if (request.getCondition() != null) product.setCondition(request.getCondition());
        if (request.getProvince() != null) product.setProvince(request.getProvince());
        if (request.getDistrict() != null) product.setDistrict(request.getDistrict());
        if (request.getFrameSize() != null) product.setFrameSize(request.getFrameSize());
        if (request.getWheelSize() != null) product.setWheelSize(request.getWheelSize());
        if (request.getGroupset() != null) product.setGroupset(request.getGroupset());

        validateRequiredTechnicalFields(product.getFrameSize(), product.getWheelSize());

        if (request.getBrandId() != null) {
            product.setBrand(brandRepository.findById(request.getBrandId()).orElse(null));
        }
        if (request.getCategoryId() != null) {
            product.setCategory(categoryRepository.findById(request.getCategoryId()).orElse(null));
        }
        if (request.getBrakeTypeId() != null) {
            product.setBrakeType(brakeTypeRepository.findById(request.getBrakeTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND)));
        }
        if (request.getFrameMaterialId() != null) {
            product.setFrameMaterial(frameMaterialRepository.findById(request.getFrameMaterialId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND)));
        }

        if (newImages != null && !newImages.isEmpty()) {
            validateMinimumImages(newImages);
            product.getImages().forEach(img -> storageService.deleteFile(img.getUrl()));
            productImageRepository.deleteAllByProductId(id);
            product.setImages(uploadImages(product, newImages));
        }

        product.setStatus(ProductStatus.pending);
        product.setExpiresAt(LocalDateTime.now().plusDays(30));
        invalidateInspection(product);

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(UUID id, User currentUser) {
        Product product = findActiveProductById(id);

        if (!product.getSeller().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        product.setDeletedAt(LocalDateTime.now());
        product.setStatus(ProductStatus.hidden);
        invalidateInspection(product);
        productRepository.save(product);
    }

    public Page<ProductResponse> getMyProducts(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.findBySellerIdAndDeletedAtIsNull(currentUser.getId(), pageable)
                .map(this::toResponse);
    }

    public Page<ProductResponse> getAllForAdmin(ProductStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status != null) {
            return productRepository.findByStatusAndDeletedAtIsNull(status, pageable).map(this::toResponse);
        }
        return productRepository.findAllByDeletedAtIsNull(pageable).map(this::toResponse);
    }

    @Transactional
    public ProductResponse changeStatus(UUID id, ProductStatus newStatus) {
        Product product = findActiveProductById(id);
        product.setStatus(newStatus);
        return toResponse(productRepository.save(product));
    }

    public ProductResponse toResponse(Product product) {
        List<ProductResponse.ImageInfo> imageInfos = product.getImages() != null
                ? product.getImages().stream()
                .map(img -> ProductResponse.ImageInfo.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .isPrimary(img.isPrimary())
                        .displayOrder(img.getDisplayOrder())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        User seller = product.getSeller();
        ProductResponse.SellerInfo sellerInfo = seller != null
                ? ProductResponse.SellerInfo.builder()
                .id(seller.getId())
                .firstName(seller.getFirstName())
                .lastName(seller.getLastName())
                .avatarUrl(seller.getAvatarUrl())
                .phone(seller.getPhone())
                .build()
                : null;

        Inspection inspection = inspectionRepository.findByProductId(product.getId()).orElse(null);
        boolean verified = isInspectionCurrentlyValid(product, inspection);
        ProductResponse.InspectionInfo inspectionInfo = inspection != null
                ? ProductResponse.InspectionInfo.builder()
                .id(inspection.getId())
                .overallScore(inspection.getOverallScore())
                .passed(inspection.getPassed())
                .reportFileUrl(inspection.getReportFileUrl())
                .validUntil(inspection.getValidUntil())
                .createdAt(inspection.getCreatedAt())
                .build()
                : null;

        return ProductResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .condition(product.getCondition())
                .status(product.getStatus())
                .province(product.getProvince())
                .district(product.getDistrict())
                .frameSize(product.getFrameSize())
                .wheelSize(product.getWheelSize())
                .groupset(product.getGroupset())
                .createdAt(product.getCreatedAt())
                .expiresAt(product.getExpiresAt())
                .seller(sellerInfo)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brakeTypeName(product.getBrakeType() != null ? product.getBrakeType().getName() : null)
                .frameMaterialName(product.getFrameMaterial() != null ? product.getFrameMaterial().getName() : null)
                .images(imageInfos)
                .isVerified(verified)
                .inspection(inspectionInfo)
                .build();
    }

    private Product findActiveProductById(UUID id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Sort buildSort(ProductFilterRequest filter) {
        if (filter == null || filter.getSortBy() == null) {
            return Sort.by("createdAt").descending();
        }
        return switch (filter.getSortBy()) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default -> Sort.by("createdAt").descending();
        };
    }

    private void validateRequiredTechnicalFields(String frameSize, String wheelSize) {
        if (frameSize == null || frameSize.isBlank() || wheelSize == null || wheelSize.isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_TECHNICAL_FIELDS_REQUIRED);
        }
    }

    private void validateMinimumImages(List<MultipartFile> images) {
        if (countNonEmptyImages(images) < 3) {
            throw new AppException(ErrorCode.PRODUCT_MINIMUM_IMAGES_REQUIRED);
        }
    }

    private long countNonEmptyImages(List<MultipartFile> images) {
        if (images == null) {
            return 0;
        }
        return images.stream()
                .filter(image -> image != null && !image.isEmpty())
                .count();
    }

    private List<ProductImage> uploadImages(Product product, List<MultipartFile> images) {
        List<ProductImage> productImages = new ArrayList<>();
        int displayOrder = 0;
        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String url = storageService.uploadFile(file, "products/" + product.getId());
            productImages.add(ProductImage.builder()
                    .product(product)
                    .url(url)
                    .isPrimary(displayOrder == 0)
                    .displayOrder(displayOrder)
                    .build());
            displayOrder++;
        }
        productImageRepository.saveAll(productImages);
        return productImages;
    }

    private void invalidateInspection(Product product) {
        inspectionRepository.findByProductId(product.getId()).ifPresent(inspection -> {
            inspection.setPassed(false);
            inspection.setValidUntil(LocalDateTime.now());
            inspectionRepository.save(inspection);
        });
    }

    private boolean isInspectionCurrentlyValid(Product product, Inspection inspection) {
        return inspection != null
                && Boolean.TRUE.equals(inspection.getPassed())
                && inspection.getValidUntil() != null
                && inspection.getValidUntil().isAfter(LocalDateTime.now())
                && product.getDeletedAt() == null
                && product.getStatus() != ProductStatus.sold
                && product.getStatus() != ProductStatus.hidden
                && product.getStatus() != ProductStatus.pending;
    }
}
