package com.pasoor.match;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchScoringTest {
    private final MatchScoring scoring = new MatchScoring();

    @Test
    void continuesBeforeEitherPlayerReachesSeventyTwo() {
        assertThat(scoring.winner(71, 70)).isEmpty();
    }

    @Test
    void winsWhenOnlyOnePlayerReachesSeventyTwo() {
        assertThat(scoring.winner(72, 60)).contains(MatchWinner.PLAYER_ONE);
        assertThat(scoring.winner(41, 72)).contains(MatchWinner.PLAYER_TWO);
    }

    @Test
    void bothExactlySeventyTwoContinues() {
        assertThat(scoring.winner(72, 72)).isEmpty();
    }

    @Test
    void bothAtOrAboveSeventyTwoAndTiedContinues() {
        assertThat(scoring.winner(80, 80)).isEmpty();
    }

    @Test
    void bothAboveSeventyTwoHigherScoreWins() {
        assertThat(scoring.winner(73, 80)).contains(MatchWinner.PLAYER_TWO);
        assertThat(scoring.winner(85, 74)).contains(MatchWinner.PLAYER_ONE);
    }
}
