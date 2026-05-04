create table game_invite (
    id uuid primary key,
    match_id uuid not null references pasoor_match (id) on delete cascade,
    sender_id uuid not null references app_user (id) on delete cascade,
    recipient_id uuid not null references app_user (id) on delete cascade,
    recipient_email varchar(320) not null,
    token varchar(96) not null unique,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    accepted_at timestamp with time zone
);

create index idx_game_invite_match on game_invite (match_id);
create index idx_game_invite_token on game_invite (token);
create index idx_game_invite_recipient on game_invite (recipient_id);
