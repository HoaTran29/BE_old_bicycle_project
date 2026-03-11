-- Thêm điểm chi tiết cho phiếu kiểm định
ALTER TABLE inspections 
ADD COLUMN frame_score INT CHECK (frame_score >= 1 AND frame_score <= 5),
ADD COLUMN fork_score INT CHECK (fork_score >= 1 AND fork_score <= 5),
ADD COLUMN brakes_score INT CHECK (brakes_score >= 1 AND brakes_score <= 5),
ADD COLUMN drivetrain_score INT CHECK (drivetrain_score >= 1 AND drivetrain_score <= 5),
ADD COLUMN wheels_score INT CHECK (wheels_score >= 1 AND wheels_score <= 5),
ADD COLUMN wear_percentage INT,
ADD COLUMN expert_notes TEXT;

-- Bảng Orders: Cập nhật status
-- order_status đã có ('pending', 'deposited', 'completed', 'cancelled')
-- app_role đã có ('guest', 'buyer', 'seller', 'inspector', 'admin')

-- Hiện tại products.status là ('pending', 'active', 'hidden', 'sold')
-- Thêm index để lọc tìm kiếm nhanh hơn
CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products(brand_id);
CREATE INDEX IF NOT EXISTS idx_products_condition ON products(condition);
