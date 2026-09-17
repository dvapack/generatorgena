create table users (
    id uuid primary key,
    email text not null unique,
    password_hash text not null
);

create table generation_requests (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    prompt text not null,
    status varchar(64) not null,
    rating int check(rating between 1 and 5),
    created_at timestamp with time zone not null default current_timestamp,
    completed_at timestamp with time zone
);

create index idx_generation_requests_user_created_id on generation_requests (user_id, created_at desc, id desc);

create table generated_assets (
    id uuid primary key,
    request_id uuid not null unique references generation_requests(id) on delete cascade,
    object_key text not null unique,
    content_type text not null,
    size_bytes int,
    created_at timestamp with time zone not null default current_timestamp
);
