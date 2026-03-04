package com.backend.old_bicycle_project.entity.enums;

public enum ProductStatus {
    pending,   // vừa đăng, chờ admin duyệt
    active,    // đã duyệt, hiển thị công khai
    hidden,    // bị ẩn bởi admin
    sold       // đã bán
}
