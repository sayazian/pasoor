create table pasoor_match (
    id uuid primary key,
    player_one_id uuid not null references app_user (id) on delete cascade,
    player_two_id uuid references app_user (id) on delete set null,
    status varchar(32) not null,
    player_one_total_score integer not null,
    player_two_total_score integer not null,
    winner_id uuid references app_user (id) on delete set null,
    winner_side varchar(32),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table game_round (
    id uuid primary key,
    match_id uuid not null references pasoor_match (id) on delete cascade,
    round_number integer not null,
    status varchar(32) not null,
    game_state_json text not null,
    player_one_round_score integer,
    player_two_round_score integer,
    created_at timestamp with time zone not null,
    finished_at timestamp with time zone
);

create index idx_pasoor_match_player_one on pasoor_match (player_one_id);
create index idx_pasoor_match_player_two on pasoor_match (player_two_id);
create index idx_game_round_match on game_round (match_id);
