ALTER TABLE pasoor_match ADD COLUMN exited_by_id UUID REFERENCES app_user(id);
ALTER TABLE pasoor_match ADD COLUMN player_one_end_choice VARCHAR(32);
ALTER TABLE pasoor_match ADD COLUMN player_two_end_choice VARCHAR(32);
ALTER TABLE pasoor_match ADD COLUMN rematch_id UUID REFERENCES pasoor_match(id);

ALTER TABLE game_round ADD COLUMN player_one_acknowledged_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE game_round ADD COLUMN player_two_acknowledged_at TIMESTAMP WITH TIME ZONE;
