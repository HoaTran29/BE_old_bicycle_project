# Manual VietQR Payout Slice - 2026-03-19

## Scope

Slice này thêm:

- `payout_profiles`
- `payouts`
- buyer refund payout manual
- seller deposit release payout manual
- admin payout list + complete payout

## Backend files chính

- `controller/PayoutProfileController.java`
- `controller/AdminPayoutController.java`
- `service/impl/PayoutServiceImpl.java`
- `service/impl/RefundServiceImpl.java`
- `service/impl/OrderServiceImpl.java`
- `entity/Payout.java`
- `entity/PayoutProfile.java`
- `db/migration/V11__manual_payout_profiles_and_records.sql`

## Frontend files chính

- `src/components/profile/PayoutProfileSection.tsx`
- `src/pages/admin/AdminPayoutsPage.tsx`
- `src/pages/ProfilePage.tsx`
- `src/lib/order-display.ts`
- `src/api/payouts.api.ts`

## Verification đã chạy

### Backend

```powershell
.\mvnw.cmd -q "-Dtest=PayoutServiceImplTest,OrderServiceImplTest,RefundServiceImplTest,SecurityConfigIntegrationTest" test
```

Kết quả:

- pass

### Frontend

```powershell
npm run test:run -- src/lib/order-display.test.ts src/components/profile/PayoutProfileSection.test.tsx src/pages/admin/AdminPayoutsPage.test.tsx
npm run build
```

Kết quả:

- pass

### Docs lint

```powershell
npm exec --yes ai-devkit@latest lint
```

Chạy ở cả BE và FE:

- pass

## Database

Migration đã được apply lên Supabase project:

- `kfkzxghznwgbbarfsqre`

Tên migration:

- `manual_payout_profiles_and_records`

## Runtime expectation

- user có thể lưu payout profile tại `GET/PUT /api/payout-profiles/me`
- admin có thể xem payout tại `GET /api/admin/payouts`
- admin có thể complete payout tại `PATCH /api/admin/payouts/{id}/complete`
- order/refund không còn nhảy thẳng sang “đã giải ngân/đã hoàn tiền” nếu chưa có `bankRef`
