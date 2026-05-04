create table friendship (
    id uuid primary key,
    requester_id uuid not null references app_user (id) on delete cascade,
    recipient_id uuid not null references app_user (id) on delete cascade,
    status varchar(32) not null,
    message varchar(500),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_friendship_requester on friendship (requester_id);
create index idx_friendship_recipient on friendship (recipient_id);
create index idx_friendship_status on friendship (status);
