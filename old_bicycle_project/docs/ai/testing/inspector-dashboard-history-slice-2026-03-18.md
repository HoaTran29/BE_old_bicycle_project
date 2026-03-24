# Inspector Dashboard History Slice - 2026-03-18

## Mục tiêu

Hoàn tất backend slice cho inspector gồm:

- dashboard
- request queue
- history
- security cho inspector/admin
- migration hỗ trợ `updated_at`

## Thay đổi chính

- Thêm migration [V10__inspection_dashboard_and_history.sql](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/resources/db/migration/V10__inspection_dashboard_and_history.sql)
- Thêm DTO:
  - `InspectionRequestItemResponseDTO`
  - `InspectionHistoryItemResponseDTO`
  - `InspectionDashboardResponseDTO`
- Mở API mới trong `InspectionController`
- Mở `InspectionSpecification`
- Nâng `InspectionRepository` để hỗ trợ `JpaSpecificationExecutor`
- Đồng bộ validation điểm từ 1..10 về 1..5

## Verify đã chạy

```powershell
.\mvnw.cmd -q "-Dtest=InspectionServiceImplTest,SecurityConfigIntegrationTest" test
```

Kết quả:

- pass

## Security checks đã cover

- anonymous không vào được `/api/inspections/dashboard`
- buyer không vào được `/api/inspections/requests`
- inspector vào được `/api/inspections/dashboard`

## Database

Migration đã được apply lên Supabase project:

- `kfkzxghznwgbbarfsqre`

Tên migration management-side dùng khi apply:

- `inspection_dashboard_and_history`

## Ghi chú

- Test vẫn có warning quen thuộc từ H2/custom enum và Mockito agent trên JDK 21
- warning không làm fail suite
