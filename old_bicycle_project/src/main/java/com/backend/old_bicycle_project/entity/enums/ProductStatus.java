package com.backend.old_bicycle_project.entity.enums;

public enum ProductStatus {
    pending,              // vừa đăng, chờ admin duyệt
    active,               // đã duyệt, hiển thị công khai
    hidden,               // bị ẩn bởi admin
    sold,                 // đã bán
    pending_inspection,   // seller đã yêu cầu kiểm định, đang chờ inspector
    inspected_passed,     // đã kiểm định và đạt yêu cầu
    inspected_failed      // đã kiểm định nhưng không đạt yêu cầu
}
