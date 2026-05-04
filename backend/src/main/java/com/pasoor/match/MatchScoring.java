package com.pasoor.match;

import java.util.Optional;

public class MatchScoring {
    private static final int WINNING_SCORE = 72;

    public Optional<MatchWinner> winner(int playerOneScore, int playerTwoScore) {
        boolean playerOneReachedTarget = playerOneScore >= WINNING_SCORE;
        boolean playerTwoReachedTarget = playerTwoScore >= WINNING_SCORE;

        if (!playerOneReachedTarget && !playerTwoReachedTarget) {
            return Optional.empty();
        }
        if (playerOneScore == playerTwoScore) {
            return Optional.empty();
        }
        if (playerOneScore >= WINNING_SCORE && playerTwoScore < WINNING_SCORE) {
            return Optional.of(MatchWinner.PLAYER_ONE);
        }
        if (playerTwoScore >= WINNING_SCORE && playerOneScore < WINNING_SCORE) {
            return Optional.of(MatchWinner.PLAYER_TWO);
        }

        return Optional.of(playerOneScore > playerTwoScore ? MatchWinner.PLAYER_ONE : MatchWinner.PLAYER_TWO);
    }
}
