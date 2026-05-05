alter table game_invite
    add column expires_at timestamp with time zone;

alter table game_invite
    add column emailed_at timestamp with time zone;

alter table game_invite
    add column last_sent_at timestamp with time zone;

alter table game_invite
    add column sent_count integer not null default 0;

update game_invite
set expires_at = created_at
where expires_at is null;

alter table game_invite
    alter column expires_at set not null;
