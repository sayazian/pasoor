create table app_user (
    id uuid primary key,
    google_subject varchar(255) not null unique,
    name varchar(255) not null,
    email varchar(320) not null unique,
    avatar_url varchar(1024),
    preferred_theme varchar(64) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_app_user_email on app_user (email);

