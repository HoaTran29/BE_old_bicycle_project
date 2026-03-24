create table if not exists size_charts (
    id uuid primary key,
    category_id uuid not null,
    name varchar(255) not null,
    description text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint fk_size_charts_category foreign key (category_id) references categories(id) on delete restrict,
    constraint uk_size_charts_category unique (category_id)
);

create table if not exists size_chart_rows (
    id uuid primary key,
    size_chart_id uuid not null,
    frame_size varchar(100) not null,
    height_min_cm integer not null,
    height_max_cm integer not null,
    note text,
    display_order integer not null default 0,
    constraint fk_size_chart_rows_chart foreign key (size_chart_id) references size_charts(id) on delete cascade,
    constraint uk_size_chart_rows_chart_frame_size unique (size_chart_id, frame_size),
    constraint chk_size_chart_rows_height_range check (height_min_cm <= height_max_cm)
);

create index if not exists idx_size_charts_category_id on size_charts(category_id);
create index if not exists idx_size_chart_rows_size_chart_id on size_chart_rows(size_chart_id);
