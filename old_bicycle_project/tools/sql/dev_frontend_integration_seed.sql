-- Dev-only frontend integration seed for Old Bicycle backend.
-- Intended for PostgreSQL / Supabase SQL Editor.
-- All seeded accounts use password: Password1
--
-- Useful tokens:
-- - Email verification token: verify-buyer-unverified-token
-- - Password reset token: reset-buyer-beta-token
--
-- Optional clean reset before re-seeding:
-- TRUNCATE TABLE
--   refund_requests,
--   reviews,
--   messages,
--   conversations,
--   notifications,
--   reports,
--   wishlists,
--   payments,
--   orders,
--   inspections,
--   product_images,
--   products,
--   password_reset_tokens,
--   email_verifications,
--   refresh_tokens,
--   categories,
--   brands,
--   brake_types,
--   frame_materials,
--   users
-- RESTART IDENTITY CASCADE;

BEGIN;

-- Shared bcrypt hash for Password1
-- $2a$10$PRIvkBDD4lDfRyMwaI2mr.oJvysqC1VqTsd/XLfHVTDV9SYelALJq

INSERT INTO users (
    id, email, password_hash, first_name, last_name, phone, avatar_url, default_address,
    role, is_verified, status, average_rating, total_reviews, created_at, updated_at
) VALUES
    ('00000000-0000-0000-0000-000000000001', 'admin@oldbicycle.dev', '$2a$10$PRIvkBDD4lDfRyMwaI2mr.oJvysqC1VqTsd/XLfHVTDV9SYelALJq', 'Hệ', 'Thống', '0900000001', 'https://picsum.photos/seed/ob-admin/256/256', '1 Admin Street, Quận 1, TP.HCM', 'admin', true, 'active', 0.0, 0, '2026-03-01 08:00:00', '2026-03-17 09:00:00'),
    ('00000000-0000-0000-0000-000000000002', 'inspector@oldbicycle.dev', '$2a$10$PRIvkBDD4lDfRyMwaI2mr.oJvysqC1VqTsd/XLfHVTDV9SYelALJq', 'Kiểm', 'Định', '0900000002', 'https://picsum.photos/seed/ob-inspector/256/256', '12 Workshop Street, Bình Thạnh, TP.HCM', 'inspector', true, 'active', 0.0, 0, '2026-03-01 08:05:00', '2026-03-17 09:00:00'),
    ('00000000-0000-0000-0000-000000000011', 'buyer.alpha@oldbicycle.dev', '$2a$10$PRIvkBDD4lDfRyMwaI2mr.oJvysqC1VqTsd/XLfHVTDV9SYelALJq', 'An', 'Buyer', '0900000011', 'https://picsum.photos/seed/ob-buyer-alpha/256/256', '25 Nguyễn Trãi, Quận 5, TP.HCM', 'buyer', true, 'active', 0.0, 0, '2026-03-02 09:00:00', '2026-03-17 09:00:00'),
    ('00000000-0000-0000-0000-000000000012', 'buyer.beta@oldbicycle.dev', '$2a$10$PRIvkBDD4lDfRyMwaI2mr.oJvysqC1VqTsd/XLfHVTDV9SYelALJq', 'Bình', 'Buyer', '0900000012', 'https://picsum.photos/seed/ob-buyer-beta/256/256', '88 Lê Lợi, Quận 1, TP.HCM', 'buyer', true, 'active', 0.0, 0, '2026-03-02 09:05:00', '2026-03-17 09:00:00'),
    ('00000000-0000-0000-0000-000000000013', 'buyer.unverified@oldbicycle.dev', '$2a$10$PRIvkBDD4lDfRyMwaI2mr.oJvysqC1VqTsd/XLfHVTDV9SYelALJq', 'Chưa', 'Xác Thực', '0900000013', 'https://picsum.photos/seed/ob-buyer-unverified/256/256', '120 Võ Văn Tần, Quận 3, TP.HCM', 'buyer', false, 'active', 0.0, 0, '2026-03-02 09:10:00', '2026-03-17 09:00:00'),
    ('00000000-0000-0000-0000-000000000014', 'buyer.banned@oldbicycle.dev', '$2a$10$PRIvkBDD4lDfRyMwaI2mr.oJvysqC1VqTsd/XLfHVTDV9SYelALJq', 'Bị', 'Khóa', '0900000014', 'https://picsum.photos/seed/ob-buyer-banned/256/256', '77 Phan Xích Long, Phú Nhuận, TP.HCM', 'buyer', true, 'banned', 0.0, 0, '2026-03-02 09:15:00', '2026-03-17 09:00:00'),
    ('00000000-0000-0000-0000-000000000021', 'seller.road@oldbicycle.dev', '$2a$10$PRIvkBDD4lDfRyMwaI2mr.oJvysqC1VqTsd/XLfHVTDV9SYelALJq', 'Road', 'Seller', '0900000021', 'https://picsum.photos/seed/ob-seller-road/256/256', '18 Pasteur, Quận 1, TP.HCM', 'seller', true, 'active', 5.0, 1, '2026-03-02 10:00:00', '2026-03-17 09:00:00'),
    ('00000000-0000-0000-0000-000000000022', 'seller.mtb@oldbicycle.dev', '$2a$10$PRIvkBDD4lDfRyMwaI2mr.oJvysqC1VqTsd/XLfHVTDV9SYelALJq', 'MTB', 'Seller', '0900000022', 'https://picsum.photos/seed/ob-seller-mtb/256/256', '33 Điện Biên Phủ, Bình Thạnh, TP.HCM', 'seller', true, 'active', 4.0, 1, '2026-03-02 10:05:00', '2026-03-17 09:00:00'),
    ('00000000-0000-0000-0000-000000000023', 'seller.city@oldbicycle.dev', '$2a$10$PRIvkBDD4lDfRyMwaI2mr.oJvysqC1VqTsd/XLfHVTDV9SYelALJq', 'City', 'Seller', '0900000023', 'https://picsum.photos/seed/ob-seller-city/256/256', '205 Nguyễn Văn Cừ, Quận 5, TP.HCM', 'seller', true, 'unactive', 0.0, 0, '2026-03-02 10:10:00', '2026-03-17 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO brake_types (id, name, description, created_at) VALUES
    ('00000000-0000-0000-0000-000000001001', 'Disc Hydraulic', 'Phanh đĩa dầu cho xe địa hình và xe đua.', '2026-03-01 08:00:00'),
    ('00000000-0000-0000-0000-000000001002', 'Disc Mechanical', 'Phanh đĩa cơ, dễ bảo trì.', '2026-03-01 08:00:00'),
    ('00000000-0000-0000-0000-000000001003', 'Rim Caliper', 'Phanh vành cho xe road.', '2026-03-01 08:00:00'),
    ('00000000-0000-0000-0000-000000001004', 'V-Brake', 'Phanh V cho city bike và MTB phổ thông.', '2026-03-01 08:00:00')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO frame_materials (id, name, description, created_at) VALUES
    ('00000000-0000-0000-0000-000000001101', 'Carbon', 'Nhẹ, cứng, phù hợp xe road hiệu năng cao.', '2026-03-01 08:00:00'),
    ('00000000-0000-0000-0000-000000001102', 'Aluminum', 'Nhôm, phổ biến cho road và MTB.', '2026-03-01 08:00:00'),
    ('00000000-0000-0000-0000-000000001103', 'Steel', 'Thép, êm và bền.', '2026-03-01 08:00:00'),
    ('00000000-0000-0000-0000-000000001104', 'Titanium', 'Titanium cho xe touring cao cấp.', '2026-03-01 08:00:00')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO brands (id, name, logo_url, created_at) VALUES
    ('00000000-0000-0000-0000-000000001201', 'Giant', 'https://picsum.photos/seed/brand-giant/300/300', '2026-03-01 08:00:00'),
    ('00000000-0000-0000-0000-000000001202', 'Trek', 'https://picsum.photos/seed/brand-trek/300/300', '2026-03-01 08:00:00'),
    ('00000000-0000-0000-0000-000000001203', 'Specialized', 'https://picsum.photos/seed/brand-specialized/300/300', '2026-03-01 08:00:00'),
    ('00000000-0000-0000-0000-000000001204', 'Cannondale', 'https://picsum.photos/seed/brand-cannondale/300/300', '2026-03-01 08:00:00')
ON CONFLICT (name) DO UPDATE
SET logo_url = EXCLUDED.logo_url;

INSERT INTO categories (id, name, slug, parent_id, created_at) VALUES
    ('00000000-0000-0000-0000-000000001301', 'Bicycles', 'bicycles', NULL, '2026-03-01 08:00:00')
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    parent_id = EXCLUDED.parent_id;

INSERT INTO categories (id, name, slug, parent_id, created_at)
SELECT seed.id, seed.name, seed.slug, parent.id, seed.created_at
FROM (
    VALUES
        ('00000000-0000-0000-0000-000000001302'::uuid, 'Road Bikes', 'road-bikes', '2026-03-01 08:01:00'::timestamp),
        ('00000000-0000-0000-0000-000000001303'::uuid, 'Mountain Bikes', 'mountain-bikes', '2026-03-01 08:01:00'::timestamp),
        ('00000000-0000-0000-0000-000000001304'::uuid, 'City Bikes', 'city-bikes', '2026-03-01 08:01:00'::timestamp),
        ('00000000-0000-0000-0000-000000001305'::uuid, 'Gravel Bikes', 'gravel-bikes', '2026-03-01 08:01:00'::timestamp)
) AS seed(id, name, slug, created_at)
JOIN categories parent
  ON parent.slug = 'bicycles'
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    parent_id = EXCLUDED.parent_id;

WITH product_seed (
    id, seller_email, brand_name, category_slug, brake_type_name, frame_material_name, title, description,
    price, original_price, frame_size, wheel_size, groupset, condition, province, district,
    status, expires_at, created_at, updated_at, deleted_at
) AS (
    VALUES
        ('00000000-0000-0000-0000-000000002001'::uuid, 'seller.road@oldbicycle.dev', 'Giant', 'road-bikes', 'Rim Caliper', 'Carbon', 'Giant TCR Advanced 2023', 'Xe road khung carbon, phù hợp người mới chơi road nhưng muốn hiệu năng tốt.', 32000000, 41000000, 'M', '700C', 'Shimano 105', 'new_90', 'TP.HCM', 'Quận 1', 'active', '2026-06-30 23:59:59'::timestamp, '2026-03-10 08:00:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp),
        ('00000000-0000-0000-0000-000000002002'::uuid, 'seller.mtb@oldbicycle.dev', 'Trek', 'mountain-bikes', 'Disc Hydraulic', 'Aluminum', 'Trek Marlin 7 2022', 'MTB nhôm, phanh dầu, phù hợp đi trail nhẹ và commuting.', 18000000, 23500000, 'M', '29"', 'Shimano Deore', 'used', 'Hà Nội', 'Cầu Giấy', 'active', '2026-06-30 23:59:59'::timestamp, '2026-03-10 08:05:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp),
        ('00000000-0000-0000-0000-000000002003'::uuid, 'seller.road@oldbicycle.dev', 'Cannondale', 'road-bikes', 'Rim Caliper', 'Aluminum', 'Cannondale CAAD Optimo 2021', 'Listing đang chờ admin duyệt sau khi seller cập nhật ảnh mới.', 14500000, 19800000, 'S', '700C', 'Tiagra', 'used', 'Đà Nẵng', 'Hải Châu', 'pending', '2026-06-30 23:59:59'::timestamp, '2026-03-11 09:00:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp),
        ('00000000-0000-0000-0000-000000002004'::uuid, 'seller.mtb@oldbicycle.dev', 'Specialized', 'mountain-bikes', 'Disc Hydraulic', 'Aluminum', 'Specialized Rockhopper 2020', 'Listing đã bị ẩn do có report cần xử lý.', 14000000, 18500000, 'M', '29"', 'SRAM SX', 'used', 'TP.HCM', 'Thủ Đức', 'hidden', '2026-06-30 23:59:59'::timestamp, '2026-03-08 10:00:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp),
        ('00000000-0000-0000-0000-000000002005'::uuid, 'seller.road@oldbicycle.dev', 'Giant', 'city-bikes', 'V-Brake', 'Steel', 'Giant Escape City 2021', 'City bike đã bán thành công và có review.', 9500000, 13500000, 'M', '700C', 'Altus', 'used', 'TP.HCM', 'Gò Vấp', 'sold', '2026-06-30 23:59:59'::timestamp, '2026-03-05 11:00:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp),
        ('00000000-0000-0000-0000-000000002006'::uuid, 'seller.mtb@oldbicycle.dev', 'Trek', 'gravel-bikes', 'Disc Mechanical', 'Aluminum', 'Trek Checkpoint ALR 2022', 'Seller đã gửi yêu cầu kiểm định, đang chờ inspector.', 21000000, 27800000, '54', '700C', 'GRX 400', 'used', 'Huế', 'Phú Nhuận', 'pending_inspection', '2026-06-30 23:59:59'::timestamp, '2026-03-09 07:30:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp),
        ('00000000-0000-0000-0000-000000002007'::uuid, 'seller.road@oldbicycle.dev', 'Specialized', 'road-bikes', 'Rim Caliper', 'Aluminum', 'Specialized Allez Sprint 2021', 'Xe đã kiểm định đạt, phù hợp buyer muốn road nhôm hiệu năng cao.', 25000000, 32000000, '54', '700C', 'Shimano 105', 'used', 'TP.HCM', 'Quận 7', 'inspected_passed', '2026-06-30 23:59:59'::timestamp, '2026-03-07 14:00:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp),
        ('00000000-0000-0000-0000-000000002008'::uuid, 'seller.mtb@oldbicycle.dev', 'Cannondale', 'mountain-bikes', 'V-Brake', 'Steel', 'Cannondale Trail 6 2020', 'Xe đã kiểm định nhưng không đạt vì khung có dấu hiệu nứt và truyền động mòn.', 9800000, 14500000, 'M', '27.5"', 'Microshift', 'needs_repair', 'Cần Thơ', 'Ninh Kiều', 'inspected_failed', '2026-06-30 23:59:59'::timestamp, '2026-03-06 15:30:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp),
        ('00000000-0000-0000-0000-000000002009'::uuid, 'seller.mtb@oldbicycle.dev', 'Giant', 'gravel-bikes', 'Disc Hydraulic', 'Aluminum', 'Giant Revolt 2023', 'Gravel bike đã hoàn tất giao dịch chuyển khoản.', 22000000, 29500000, 'M', '700C', 'GRX 600', 'new_90', 'Hà Nội', 'Tây Hồ', 'sold', '2026-06-30 23:59:59'::timestamp, '2026-03-04 10:00:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp),
        ('00000000-0000-0000-0000-000000002010'::uuid, 'seller.road@oldbicycle.dev', 'Cannondale', 'city-bikes', 'V-Brake', 'Steel', 'Touring City Step-Through 2022', 'Case đang có refund đã được admin approve để FE test refund badge.', 11000000, 15900000, 'M', '700C', 'Shimano Altus', 'used', 'TP.HCM', 'Quận 10', 'sold', '2026-06-30 23:59:59'::timestamp, '2026-03-03 08:30:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp),
        ('00000000-0000-0000-0000-000000002011'::uuid, 'seller.mtb@oldbicycle.dev', 'Specialized', 'gravel-bikes', 'Disc Hydraulic', 'Carbon', 'Specialized Diverge E5 2022', 'Case refund đã hoàn tất để FE test timeline hoàn tiền.', 28000000, 35500000, '54', '700C', 'GRX 600', 'new_90', 'Đà Nẵng', 'Sơn Trà', 'sold', '2026-06-30 23:59:59'::timestamp, '2026-03-02 16:00:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp),
        ('00000000-0000-0000-0000-000000002012'::uuid, 'seller.road@oldbicycle.dev', 'Trek', 'road-bikes', 'Rim Caliper', 'Aluminum', 'Trek Domane AL 4 2023', 'Xe road còn active, đang có một order chuyển khoản đã đặt cọc thành công.', 25000000, 31500000, '54', '700C', 'Tiagra', 'new_90', 'TP.HCM', 'Quận 3', 'active', '2026-06-30 23:59:59'::timestamp, '2026-03-12 10:00:00'::timestamp, '2026-03-17 09:00:00'::timestamp, NULL::timestamp)
)
INSERT INTO products (
    id, seller_id, brand_id, category_id, brake_type_id, frame_material_id, title, description,
    price, original_price, frame_size, wheel_size, groupset, condition, province, district,
    status, expires_at, created_at, updated_at, deleted_at
)
SELECT
    seed.id,
    seller.id,
    brand.id,
    category.id,
    brake_type.id,
    frame_material.id,
    seed.title,
    seed.description,
    seed.price,
    seed.original_price,
    seed.frame_size,
    seed.wheel_size,
    seed.groupset,
    seed.condition::condition_type,
    seed.province,
    seed.district,
    seed.status::product_status,
    seed.expires_at,
    seed.created_at,
    seed.updated_at,
    seed.deleted_at
FROM product_seed seed
JOIN users seller
  ON seller.email = seed.seller_email
JOIN brands brand
  ON brand.name = seed.brand_name
JOIN categories category
  ON category.slug = seed.category_slug
JOIN brake_types brake_type
  ON brake_type.name = seed.brake_type_name
JOIN frame_materials frame_material
  ON frame_material.name = seed.frame_material_name
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_images (id, product_id, url, is_primary, display_order, created_at) VALUES
    ('00000000-0000-0000-0000-000000003001', '00000000-0000-0000-0000-000000002001', 'https://picsum.photos/seed/ob-p1-main/1200/900', true, 0, '2026-03-10 08:01:00'),
    ('00000000-0000-0000-0000-000000003002', '00000000-0000-0000-0000-000000002001', 'https://picsum.photos/seed/ob-p1-side/1200/900', false, 1, '2026-03-10 08:02:00'),
    ('00000000-0000-0000-0000-000000003003', '00000000-0000-0000-0000-000000002002', 'https://picsum.photos/seed/ob-p2-main/1200/900', true, 0, '2026-03-10 08:06:00'),
    ('00000000-0000-0000-0000-000000003004', '00000000-0000-0000-0000-000000002002', 'https://picsum.photos/seed/ob-p2-detail/1200/900', false, 1, '2026-03-10 08:07:00'),
    ('00000000-0000-0000-0000-000000003005', '00000000-0000-0000-0000-000000002003', 'https://picsum.photos/seed/ob-p3-main/1200/900', true, 0, '2026-03-11 09:01:00'),
    ('00000000-0000-0000-0000-000000003006', '00000000-0000-0000-0000-000000002004', 'https://picsum.photos/seed/ob-p4-main/1200/900', true, 0, '2026-03-08 10:01:00'),
    ('00000000-0000-0000-0000-000000003007', '00000000-0000-0000-0000-000000002005', 'https://picsum.photos/seed/ob-p5-main/1200/900', true, 0, '2026-03-05 11:01:00'),
    ('00000000-0000-0000-0000-000000003008', '00000000-0000-0000-0000-000000002006', 'https://picsum.photos/seed/ob-p6-main/1200/900', true, 0, '2026-03-09 07:31:00'),
    ('00000000-0000-0000-0000-000000003009', '00000000-0000-0000-0000-000000002007', 'https://picsum.photos/seed/ob-p7-main/1200/900', true, 0, '2026-03-07 14:01:00'),
    ('00000000-0000-0000-0000-000000003010', '00000000-0000-0000-0000-000000002008', 'https://picsum.photos/seed/ob-p8-main/1200/900', true, 0, '2026-03-06 15:31:00'),
    ('00000000-0000-0000-0000-000000003011', '00000000-0000-0000-0000-000000002009', 'https://picsum.photos/seed/ob-p9-main/1200/900', true, 0, '2026-03-04 10:01:00'),
    ('00000000-0000-0000-0000-000000003012', '00000000-0000-0000-0000-000000002010', 'https://picsum.photos/seed/ob-p10-main/1200/900', true, 0, '2026-03-03 08:31:00'),
    ('00000000-0000-0000-0000-000000003013', '00000000-0000-0000-0000-000000002011', 'https://picsum.photos/seed/ob-p11-main/1200/900', true, 0, '2026-03-02 16:01:00'),
    ('00000000-0000-0000-0000-000000003014', '00000000-0000-0000-0000-000000002012', 'https://picsum.photos/seed/ob-p12-main/1200/900', true, 0, '2026-03-12 10:01:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inspections (
    id, product_id, inspector_id, overall_score, frame_score, fork_score, brakes_score,
    drivetrain_score, wheels_score, wear_percentage, expert_notes, passed, report_file_url,
    valid_until, created_at
) VALUES
    ('00000000-0000-0000-0000-000000004001', '00000000-0000-0000-0000-000000002007', '00000000-0000-0000-0000-000000000002', 4.6, 5, 4, 4, 5, 5, 18, 'Khung ổn, truyền động tốt, có thể giao dịch bình thường.', true, 'https://example.com/reports/inspection-4001.pdf', '2026-06-01 00:00:00', '2026-03-13 09:00:00'),
    ('00000000-0000-0000-0000-000000004002', '00000000-0000-0000-0000-000000002008', '00000000-0000-0000-0000-000000000002', 2.1, 1, 2, 3, 2, 2, 72, 'Khung có dấu hiệu nứt, bộ truyền động mòn mạnh.', false, 'https://example.com/reports/inspection-4002.pdf', '2026-04-15 00:00:00', '2026-03-13 10:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO orders (
    id, buyer_id, product_id, seller_id, total_amount, deposit_amount, required_upfront_amount,
    paid_amount, remaining_amount, service_fee, payment_option, status, funding_status,
    payment_method, accepted_at, payment_deadline, created_at, updated_at
) VALUES
    ('00000000-0000-0000-0000-000000005001', '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000002001', '00000000-0000-0000-0000-000000000021', 32000000, 3200000, 3200000, 0, 28800000, 320000, 'partial', 'pending', 'unpaid', 'cash', NULL, NULL, '2026-03-16 09:00:00', '2026-03-16 09:00:00'),
    ('00000000-0000-0000-0000-000000005002', '00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000002005', '00000000-0000-0000-0000-000000000021', 9500000, 950000, 950000, 950000, 8550000, 95000, 'partial', 'completed', 'released', 'cash', '2026-03-06 09:00:00', '2026-03-08 23:59:59', '2026-03-06 08:30:00', '2026-03-10 18:00:00'),
    ('00000000-0000-0000-0000-000000005003', '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000002007', '00000000-0000-0000-0000-000000000021', 25000000, 5000000, 5000000, 5000000, 20000000, 250000, 'partial', 'deposited', 'held', 'transfer', '2026-03-14 10:00:00', '2026-03-15 23:59:59', '2026-03-14 09:30:00', '2026-03-15 09:00:00'),
    ('00000000-0000-0000-0000-000000005004', '00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000002009', '00000000-0000-0000-0000-000000000022', 22000000, 4400000, 4400000, 4400000, 17600000, 220000, 'partial', 'completed', 'released', 'transfer', '2026-03-07 14:00:00', '2026-03-08 23:59:59', '2026-03-07 13:00:00', '2026-03-12 17:00:00'),
    ('00000000-0000-0000-0000-000000005005', '00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000000022', 18000000, 3600000, 3600000, 0, 18000000, 180000, 'partial', 'cancelled', 'unpaid', 'transfer', '2026-03-13 09:00:00', '2026-03-14 23:59:59', '2026-03-13 08:00:00', '2026-03-14 20:00:00'),
    ('00000000-0000-0000-0000-000000005006', '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000002010', '00000000-0000-0000-0000-000000000021', 11000000, 2200000, 2200000, 2200000, 8800000, 110000, 'partial', 'deposited', 'refund_pending', 'transfer', '2026-03-11 11:00:00', '2026-03-12 23:59:59', '2026-03-11 10:00:00', '2026-03-16 09:00:00'),
    ('00000000-0000-0000-0000-000000005007', '00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000002011', '00000000-0000-0000-0000-000000000022', 28000000, 5600000, 5600000, 5600000, 22400000, 280000, 'partial', 'cancelled', 'refunded', 'transfer', '2026-03-09 10:00:00', '2026-03-10 23:59:59', '2026-03-09 09:00:00', '2026-03-16 12:00:00'),
    ('00000000-0000-0000-0000-000000005008', '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000002012', '00000000-0000-0000-0000-000000000021', 25000000, 5000000, 5000000, 5000000, 20000000, 250000, 'partial', 'deposited', 'held', 'transfer', '2026-03-16 11:00:00', '2026-03-17 23:59:59', '2026-03-16 10:30:00', '2026-03-17 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO payments (
    id, order_id, amount, gateway, method, phase, status, gateway_order_code, checkout_url,
    qr_code_url, transaction_reference, gateway_response, payment_date, created_at, updated_at
) VALUES
    ('00000000-0000-0000-0000-000000006003', '00000000-0000-0000-0000-000000005003', 5000000, 'sepay', 'transfer', 'upfront', 'success', 'OB-ORD-5003', 'https://pay.example.com/checkout/5003', 'https://pay.example.com/qr/5003', 'SEPAY-TXN-5003', '{"gateway":"sepay","status":"success","amount":5000000}'::jsonb, '2026-03-14 10:30:00', '2026-03-14 10:00:00', '2026-03-14 10:30:00'),
    ('00000000-0000-0000-0000-000000006004', '00000000-0000-0000-0000-000000005004', 4400000, 'sepay', 'transfer', 'upfront', 'success', 'OB-ORD-5004', 'https://pay.example.com/checkout/5004', 'https://pay.example.com/qr/5004', 'SEPAY-TXN-5004', '{"gateway":"sepay","status":"success","amount":4400000}'::jsonb, '2026-03-07 14:20:00', '2026-03-07 14:00:00', '2026-03-07 14:20:00'),
    ('00000000-0000-0000-0000-000000006005', '00000000-0000-0000-0000-000000005005', 3600000, 'sepay', 'transfer', 'upfront', 'failed', 'OB-ORD-5005', 'https://pay.example.com/checkout/5005', 'https://pay.example.com/qr/5005', 'SEPAY-TXN-5005', '{"gateway":"sepay","status":"failed","amount":3600000}'::jsonb, NULL, '2026-03-13 08:30:00', '2026-03-14 20:00:00'),
    ('00000000-0000-0000-0000-000000006006', '00000000-0000-0000-0000-000000005006', 2200000, 'sepay', 'transfer', 'upfront', 'success', 'OB-ORD-5006', 'https://pay.example.com/checkout/5006', 'https://pay.example.com/qr/5006', 'SEPAY-TXN-5006', '{"gateway":"sepay","status":"success","amount":2200000}'::jsonb, '2026-03-11 11:20:00', '2026-03-11 11:00:00', '2026-03-16 09:00:00'),
    ('00000000-0000-0000-0000-000000006007', '00000000-0000-0000-0000-000000005007', 5600000, 'sepay', 'transfer', 'upfront', 'refunded', 'OB-ORD-5007', 'https://pay.example.com/checkout/5007', 'https://pay.example.com/qr/5007', 'SEPAY-TXN-5007', '{"gateway":"sepay","status":"refunded","amount":5600000}'::jsonb, '2026-03-09 10:20:00', '2026-03-09 10:00:00', '2026-03-16 12:00:00'),
    ('00000000-0000-0000-0000-000000006008', '00000000-0000-0000-0000-000000005008', 5000000, 'sepay', 'transfer', 'upfront', 'success', 'OB-ORD-5008', 'https://pay.example.com/checkout/5008', 'https://pay.example.com/qr/5008', 'SEPAY-TXN-5008', '{"gateway":"sepay","status":"success","amount":5000000}'::jsonb, '2026-03-16 11:20:00', '2026-03-16 11:00:00', '2026-03-16 11:20:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO refund_requests (
    id, order_id, payment_id, requester_id, amount, reason, evidence_note, status,
    admin_note, refund_reference, reviewed_by, reviewed_at, processed_at, created_at, updated_at
) VALUES
    ('00000000-0000-0000-0000-000000007001', '00000000-0000-0000-0000-000000005006', '00000000-0000-0000-0000-000000006006', '00000000-0000-0000-0000-000000000011', 2200000, 'Buyer đổi ý trước khi nhận xe.', 'Buyer gửi ảnh chat xác nhận muốn hủy.', 'approved', 'Admin đã approve, chờ xử lý hoàn tiền thực tế.', NULL, '00000000-0000-0000-0000-000000000001', '2026-03-16 08:30:00', NULL, '2026-03-15 09:00:00', '2026-03-16 08:30:00'),
    ('00000000-0000-0000-0000-000000007002', '00000000-0000-0000-0000-000000005007', '00000000-0000-0000-0000-000000006007', '00000000-0000-0000-0000-000000000012', 5600000, 'Xe nhận được không đúng mô tả ban đầu.', 'Buyer đã cung cấp ảnh thực tế và report inspection sau giao dịch.', 'completed', 'Đã hoàn tiền thành công qua tham chiếu refund của SePay.', 'SEPAY-REF-7002', '00000000-0000-0000-0000-000000000001', '2026-03-15 15:00:00', '2026-03-16 12:00:00', '2026-03-15 10:00:00', '2026-03-16 12:00:00'),
    ('00000000-0000-0000-0000-000000007003', '00000000-0000-0000-0000-000000005004', '00000000-0000-0000-0000-000000006004', '00000000-0000-0000-0000-000000000012', 4400000, 'Buyer muốn đổi xe khác nhưng seller không đồng ý.', 'Không có bằng chứng về lỗi sản phẩm.', 'rejected', 'Không đủ căn cứ hoàn tiền vì giao dịch đã hoàn tất đúng mô tả.', NULL, '00000000-0000-0000-0000-000000000001', '2026-03-13 16:00:00', NULL, '2026-03-13 14:00:00', '2026-03-13 16:00:00'),
    ('00000000-0000-0000-0000-000000007004', '00000000-0000-0000-0000-000000005003', '00000000-0000-0000-0000-000000006003', '00000000-0000-0000-0000-000000000011', 5000000, 'Buyer nghi ngờ xe có lỗi phát sinh sau khi đặt cọc.', 'Buyer đã mở dispute và đang chờ admin xem xét.', 'pending', 'Chưa review', NULL, NULL, NULL, NULL, '2026-03-16 14:00:00', '2026-03-16 14:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO conversations (id, product_id, buyer_id, seller_id, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-000000008001', '00000000-0000-0000-0000-000000002001', '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000021', '2026-03-16 08:00:00', '2026-03-16 08:10:00'),
    ('00000000-0000-0000-0000-000000008002', '00000000-0000-0000-0000-000000002009', '00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000022', '2026-03-07 12:00:00', '2026-03-07 12:30:00'),
    ('00000000-0000-0000-0000-000000008003', '00000000-0000-0000-0000-000000002007', '00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000021', '2026-03-14 08:00:00', '2026-03-16 14:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO messages (id, conversation_id, sender_id, content, image_url, is_read, created_at) VALUES
    ('00000000-0000-0000-0000-000000008101', '00000000-0000-0000-0000-000000008001', '00000000-0000-0000-0000-000000000011', 'Xe này còn không anh?', NULL, true, '2026-03-16 08:00:00'),
    ('00000000-0000-0000-0000-000000008102', '00000000-0000-0000-0000-000000008001', '00000000-0000-0000-0000-000000000021', 'Còn bạn nhé, xe vẫn đang ở shop.', NULL, true, '2026-03-16 08:02:00'),
    ('00000000-0000-0000-0000-000000008103', '00000000-0000-0000-0000-000000008001', '00000000-0000-0000-0000-000000000011', 'Mình có thể xem thêm ảnh groupset được không?', NULL, true, '2026-03-16 08:05:00'),
    ('00000000-0000-0000-0000-000000008104', '00000000-0000-0000-0000-000000008001', '00000000-0000-0000-0000-000000000021', 'Mình vừa gửi thêm ảnh rồi nhé.', 'https://picsum.photos/seed/ob-chat-8104/1200/900', false, '2026-03-16 08:10:00'),
    ('00000000-0000-0000-0000-000000008105', '00000000-0000-0000-0000-000000008002', '00000000-0000-0000-0000-000000000012', 'Mình muốn chốt xe này trong hôm nay.', NULL, true, '2026-03-07 12:00:00'),
    ('00000000-0000-0000-0000-000000008106', '00000000-0000-0000-0000-000000008002', '00000000-0000-0000-0000-000000000022', 'Ok, mình giữ xe cho bạn tới tối.', NULL, true, '2026-03-07 12:05:00'),
    ('00000000-0000-0000-0000-000000008107', '00000000-0000-0000-0000-000000008003', '00000000-0000-0000-0000-000000000011', 'Xe đã inspection pass đúng không shop?', NULL, true, '2026-03-16 13:40:00'),
    ('00000000-0000-0000-0000-000000008108', '00000000-0000-0000-0000-000000008003', '00000000-0000-0000-0000-000000000021', 'Đúng rồi, mình có report PDF nếu bạn cần.', NULL, false, '2026-03-16 14:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO wishlists (user_id, product_id, created_at) VALUES
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000002001', '2026-03-15 09:00:00'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000002002', '2026-03-15 09:01:00'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000002012', '2026-03-15 09:02:00'),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000002001', '2026-03-12 08:00:00'),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000002003', '2026-03-12 08:01:00'),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000002006', '2026-03-12 08:02:00')
ON CONFLICT (user_id, product_id) DO NOTHING;

INSERT INTO reports (
    id, reporter_id, processed_by, target_id, target_type, reason, description,
    admin_note, status, created_at, processed_at
) VALUES
    ('00000000-0000-0000-0000-000000009001', '00000000-0000-0000-0000-000000000011', NULL, '00000000-0000-0000-0000-000000002004', 'product', 'wrong_description', 'Mô tả nói xe zin nhưng ảnh cho thấy đã thay groupset.', NULL, 'pending', '2026-03-16 10:00:00', NULL),
    ('00000000-0000-0000-0000-000000009002', '00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000023', 'user', 'spam', 'Seller trả lời chậm và gửi nhiều tin nhắn quảng cáo.', 'Đã nhắc nhở seller và theo dõi thêm.', 'reviewed', '2026-03-13 11:00:00', '2026-03-14 09:00:00'),
    ('00000000-0000-0000-0000-000000009003', '00000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000002008', 'product', 'fake', 'Listing nghi ngờ dùng ảnh không đúng tình trạng thật.', 'Admin đã resolve sau khi hidden listing.', 'resolved', '2026-03-12 15:00:00', '2026-03-13 08:30:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO notifications (id, user_id, title, content, type, is_read, metadata, created_at) VALUES
    ('00000000-0000-0000-0000-000000009101', '00000000-0000-0000-0000-000000000011', 'Đơn hàng đã được chấp nhận', 'Seller đã chấp nhận đơn mua Trek Domane AL 4 của bạn.', 'order', false, '{"orderId":"00000000-0000-0000-0000-000000005008"}'::jsonb, '2026-03-16 10:45:00'),
    ('00000000-0000-0000-0000-000000009102', '00000000-0000-0000-0000-000000000011', 'Có tin nhắn mới', 'Seller vừa trả lời trong cuộc trò chuyện về Giant TCR Advanced 2023.', 'chat', false, '{"conversationId":"00000000-0000-0000-0000-000000008001"}'::jsonb, '2026-03-16 08:10:00'),
    ('00000000-0000-0000-0000-000000009103', '00000000-0000-0000-0000-000000000012', 'Hoàn tiền bị từ chối', 'Yêu cầu hoàn tiền cho Giant Revolt 2023 đã bị từ chối.', 'system', true, '{"refundId":"00000000-0000-0000-0000-000000007003"}'::jsonb, '2026-03-13 16:05:00'),
    ('00000000-0000-0000-0000-000000009104', '00000000-0000-0000-0000-000000000021', 'Có người quan tâm sản phẩm của bạn', 'Buyer vừa thêm Giant TCR Advanced 2023 vào wishlist.', 'wishlist', false, '{"productId":"00000000-0000-0000-0000-000000002001"}'::jsonb, '2026-03-15 09:03:00'),
    ('00000000-0000-0000-0000-000000009105', '00000000-0000-0000-0000-000000000021', 'Có tin nhắn mới', 'Buyer đang hỏi thêm về Specialized Allez Sprint 2021.', 'chat', true, '{"conversationId":"00000000-0000-0000-0000-000000008003"}'::jsonb, '2026-03-16 14:01:00'),
    ('00000000-0000-0000-0000-000000009106', '00000000-0000-0000-0000-000000000022', 'Đơn hàng đã hoàn tất', 'Giao dịch Giant Revolt 2023 đã hoàn tất thành công.', 'order', true, '{"orderId":"00000000-0000-0000-0000-000000005004"}'::jsonb, '2026-03-12 17:05:00'),
    ('00000000-0000-0000-0000-000000009107', '00000000-0000-0000-0000-000000000022', 'Yêu cầu kiểm định mới', 'Trek Checkpoint ALR 2022 đang chờ inspector xử lý.', 'inspection', false, '{"productId":"00000000-0000-0000-0000-000000002006"}'::jsonb, '2026-03-09 07:35:00'),
    ('00000000-0000-0000-0000-000000009108', '00000000-0000-0000-0000-000000000023', 'Tài khoản cần xác minh thêm', 'Admin đang theo dõi tài khoản của bạn sau báo cáo spam.', 'system', false, '{"reportId":"00000000-0000-0000-0000-000000009002"}'::jsonb, '2026-03-14 09:05:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO reviews (id, order_id, reviewer_id, reviewee_id, rating, comment, created_at) VALUES
    ('00000000-0000-0000-0000-000000009201', '00000000-0000-0000-0000-000000005002', '00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000021', 5, 'Seller phản hồi nhanh, xe đúng mô tả và giao dịch mượt.', '2026-03-10 19:00:00'),
    ('00000000-0000-0000-0000-000000009202', '00000000-0000-0000-0000-000000005004', '00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000022', 4, 'Xe tốt, chỉ hơi trễ hẹn một chút nhưng overall ổn.', '2026-03-12 18:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO email_verifications (id, user_id, token, expires_at, created_at) VALUES
    ('00000000-0000-0000-0000-000000009301', '00000000-0000-0000-0000-000000000013', 'verify-buyer-unverified-token', '2026-03-18 23:59:59', '2026-03-17 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO password_reset_tokens (id, user_id, token, expires_at, created_at) VALUES
    ('00000000-0000-0000-0000-000000009401', '00000000-0000-0000-0000-000000000012', 'reset-buyer-beta-token', '2026-03-18 23:59:59', '2026-03-17 09:05:00')
ON CONFLICT (id) DO NOTHING;

COMMIT;
