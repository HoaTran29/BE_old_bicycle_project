package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.dto.product.*;
import com.backend.old_bicycle_project.entity.*;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.repository.*;
import com.backend.old_bicycle_project.specification.ProductSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final BrakeTypeRepository brakeTypeRepository;
    private final FrameMaterialRepository frameMaterialRepository;
    private final StorageService storageService;

    // ==================== PUBLIC: LIST & SEARCH ====================

    public Page<ProductResponse> searchProducts(ProductFilterRequest filter, int page, int size) {
        Specification<Product> spec = ProductSpecification.fromFilter(filter);

        Sort sort = buildSort(filter);
        Pageable pageable = PageRequest.of(page, size, sort);

        return productRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    public ProductResponse getById(UUID id) {
        Product product = findProductById(id);
        return toResponse(product);
    }

    // ==================== SELLER: CRUD ====================

    @Transactional
    public ProductResponse create(ProductCreateRequest request, List<MultipartFile> images, User seller) {
        BrakeType brakeType = brakeTypeRepository.findById(request.getBrakeTypeId())
                .orElseThrow(() -> new RuntimeException("Loại phanh không hợp lệ"));
        FrameMaterial frameMaterial = frameMaterialRepository.findById(request.getFrameMaterialId())
                .orElseThrow(() -> new RuntimeException("Chất liệu khung không hợp lệ"));

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
                .status(ProductStatus.pending) // Chờ admin duyệt
                .build();

        productRepository.save(product);

        // Upload ảnh lên Supabase Storage
        if (images != null && !images.isEmpty()) {
            List<ProductImage> productImages = new ArrayList<>();
            for (int i = 0; i < images.size(); i++) {
                MultipartFile file = images.get(i);
                if (!file.isEmpty()) {
                    String url = storageService.uploadFile(file, "products/" + product.getId());
                    productImages.add(ProductImage.builder()
                            .product(product)
                            .url(url)
                            .isPrimary(i == 0)
                            .displayOrder(i)
                            .build());
                }
            }
            productImageRepository.saveAll(productImages);
            product.setImages(productImages);
        }

        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductUpdateRequest request, List<MultipartFile> newImages, User currentUser) {
        Product product = findProductById(id);

        // Kiểm tra quyền sở hữu
        if (!product.getSeller().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền sửa tin này");
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

        if (request.getBrandId() != null) {
            product.setBrand(brandRepository.findById(request.getBrandId()).orElse(null));
        }
        if (request.getCategoryId() != null) {
            product.setCategory(categoryRepository.findById(request.getCategoryId()).orElse(null));
        }
        if (request.getBrakeTypeId() != null) {
            product.setBrakeType(brakeTypeRepository.findById(request.getBrakeTypeId())
                    .orElseThrow(() -> new RuntimeException("Loại phanh không hợp lệ")));
        }
        if (request.getFrameMaterialId() != null) {
            product.setFrameMaterial(frameMaterialRepository.findById(request.getFrameMaterialId())
                    .orElseThrow(() -> new RuntimeException("Chất liệu khung không hợp lệ")));
        }

        // Nếu có ảnh mới, xóa ảnh cũ và upload mới
        if (newImages != null && !newImages.isEmpty()) {
            product.getImages().forEach(img -> storageService.deleteFile(img.getUrl()));
            productImageRepository.deleteAllByProductId(id);

            List<ProductImage> updatedImages = new ArrayList<>();
            for (int i = 0; i < newImages.size(); i++) {
                MultipartFile file = newImages.get(i);
                if (!file.isEmpty()) {
                    String url = storageService.uploadFile(file, "products/" + product.getId());
                    updatedImages.add(ProductImage.builder()
                            .product(product)
                            .url(url)
                            .isPrimary(i == 0)
                            .displayOrder(i)
                            .build());
                }
            }
            productImageRepository.saveAll(updatedImages);
            product.setImages(updatedImages);
        }

        // Khi seller sửa, reset về pending để admin duyệt lại
        product.setStatus(ProductStatus.pending);
        productRepository.save(product);

        return toResponse(product);
    }

    @Transactional
    public void delete(UUID id, User currentUser) {
        Product product = findProductById(id);
        if (!product.getSeller().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa tin này");
        }
        // Xóa ảnh trên Supabase Storage
        product.getImages().forEach(img -> storageService.deleteFile(img.getUrl()));
        productRepository.delete(product);
    }

    // ==================== ADMIN ====================

    public Page<ProductResponse> getAllForAdmin(ProductStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status != null) {
            return productRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public ProductResponse changeStatus(UUID id, ProductStatus newStatus) {
        Product product = findProductById(id);
        product.setStatus(newStatus);
        return toResponse(productRepository.save(product));
    }

    // ==================== HELPERS ====================

    private Product findProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
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
                .isVerified(false) // Sẽ được set = true bởi InspectionService sau này
                .build();
    }
}
