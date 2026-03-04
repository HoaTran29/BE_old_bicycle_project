-- Kích hoạt extension để tạo UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Định nghĩa các kiểu ENUM
CREATE TYPE app_role AS ENUM ('guest', 'buyer', 'seller', 'inspector', 'admin');
CREATE TYPE user_status AS ENUM ('active', 'unactive', 'banned');
CREATE TYPE product_status AS ENUM ('pending', 'active', 'hidden', 'sold');
CREATE TYPE condition_type AS ENUM ('new_90', 'used', 'needs_repair'); -- Đổi tên vì 'condition' là từ khóa hệ thống
CREATE TYPE order_status AS ENUM ('pending', 'deposited', 'completed', 'cancelled');
CREATE TYPE payment_method AS ENUM ('transfer', 'cash', 'online');
CREATE TYPE report_reason AS ENUM ('fraud', 'fake', 'wrong_description', 'spam', 'other');
CREATE TYPE report_status AS ENUM ('pending', 'reviewed', 'resolved');
CREATE TYPE payment_status AS ENUM ('pending', 'processing', 'success', 'failed', 'refunded');
CREATE TYPE notification_type AS ENUM ('order', 'chat', 'system', 'inspection', 'promotion', 'wishlist');

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    email varchar UNIQUE NOT NULL,
    password_hash varchar NOT NULL,
    first_name varchar,
    last_name varchar,
    phone varchar,
    avatar_url text,
    default_address text,
    role app_role DEFAULT 'buyer',
    is_verified boolean DEFAULT false,
    status user_status DEFAULT 'active',
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now()
);

CREATE TABLE brake_types (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name varchar UNIQUE NOT NULL,
    description text,
    created_at timestamp DEFAULT now()
);

CREATE TABLE frame_materials (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name varchar UNIQUE NOT NULL,
    description text,
    created_at timestamp DEFAULT now()
);

CREATE TABLE brands (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name varchar UNIQUE NOT NULL,
    logo_url text,
    created_at timestamp DEFAULT now()
);

CREATE TABLE categories (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    name varchar NOT NULL,
    slug varchar UNIQUE NOT NULL,
    parent_id uuid REFERENCES categories(id) ON DELETE SET NULL,
    created_at timestamp DEFAULT now()
);

CREATE TABLE products (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    seller_id uuid REFERENCES users(id) ON DELETE CASCADE,
    brand_id uuid REFERENCES brands(id),
    category_id uuid REFERENCES categories(id),
    brake_type_id uuid NOT NULL REFERENCES brake_types(id),
    frame_material_id uuid NOT NULL REFERENCES frame_materials(id),
    title varchar NOT NULL,
    description text,
    price numeric NOT NULL,
    original_price numeric,
    frame_size varchar,
    wheel_size varchar,
    groupset varchar,
    condition condition_type DEFAULT 'used',
    province varchar,
    district varchar,
    status product_status DEFAULT 'pending',
    expires_at timestamp,
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now()
);

CREATE TABLE product_images (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id uuid REFERENCES products(id) ON DELETE CASCADE,
    url text NOT NULL,
    is_primary boolean DEFAULT false,
    display_order int DEFAULT 0,
    created_at timestamp DEFAULT now()
);

CREATE TABLE inspections (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id uuid REFERENCES products(id) ON DELETE CASCADE,
    inspector_id uuid REFERENCES users(id),
    overall_score numeric(3,1),
    passed boolean DEFAULT false,
    report_file_url text,
    valid_until timestamp,
    created_at timestamp DEFAULT now()
);

CREATE TABLE orders (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    buyer_id uuid REFERENCES users(id),
    product_id uuid REFERENCES products(id),
    seller_id uuid REFERENCES users(id),
    total_amount numeric NOT NULL,
    deposit_amount numeric,
    service_fee numeric,
    status order_status DEFAULT 'pending',
    payment_method payment_method,
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now()
);

CREATE TABLE payments (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id uuid NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    amount numeric NOT NULL,
    method payment_method NOT NULL,
    status payment_status DEFAULT 'pending',
    transaction_reference varchar UNIQUE,
    gateway_response jsonb,
    payment_date timestamp,
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now()
);

CREATE TABLE conversations (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id uuid REFERENCES products(id) ON DELETE CASCADE,
    buyer_id uuid REFERENCES users(id),
    seller_id uuid REFERENCES users(id),
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now()
);

CREATE TABLE messages (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id uuid REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id uuid REFERENCES users(id),
    content text,
    image_url text,
    is_read boolean DEFAULT false,
    created_at timestamp DEFAULT now()
);

CREATE TABLE reviews (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id uuid REFERENCES orders(id) ON DELETE CASCADE,
    reviewer_id uuid REFERENCES users(id),
    reviewee_id uuid REFERENCES users(id),
    rating int CHECK (rating >= 1 AND rating <= 5),
    comment text,
    created_at timestamp DEFAULT now()
);

CREATE TABLE notifications (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title varchar NOT NULL,
    content text,
    type notification_type NOT NULL,
    is_read boolean DEFAULT false,
    metadata jsonb,
    created_at timestamp DEFAULT now()
);

CREATE TABLE reports (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id uuid REFERENCES users(id),
    target_id uuid NOT NULL, -- ID của Product hoặc User bị báo cáo
    target_type varchar NOT NULL, -- 'product' hoặc 'user'
    reason report_reason,
    description text,
    status report_status DEFAULT 'pending',
    created_at timestamp DEFAULT now()
);

CREATE TABLE wishlists (
    user_id uuid REFERENCES users(id) ON DELETE CASCADE,
    product_id uuid REFERENCES products(id) ON DELETE CASCADE,
    created_at timestamp DEFAULT now(),
    PRIMARY KEY (user_id, product_id)
);

