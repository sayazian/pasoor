alter table app_user
    add column last_seen_at timestamp with time zone;

alter table game_invite
    alter column expires_at drop not null;
