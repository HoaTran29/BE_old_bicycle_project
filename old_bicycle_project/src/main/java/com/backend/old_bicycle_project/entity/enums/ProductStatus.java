package com.backend.old_bicycle_project.entity.enums;

public enum ProductStatus {
    pending,              // vừa đăng, chờ admin kiểm duyệt nội dung ban đầu
    active,               // đã kiểm định đạt và đang hiển thị công khai
    hidden,               // bị ẩn bởi admin hoặc seller
    sold,                 // đã bán
    pending_inspection,   // admin đã chuyển tin sang hàng chờ inspector
    inspected_passed,     // trạng thái legacy: đã kiểm định đạt
    inspected_failed      // đã kiểm định nhưng không đạt
}
