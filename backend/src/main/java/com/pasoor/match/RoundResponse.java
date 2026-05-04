package com.pasoor.match;

import com.pasoor.game.GameState;

import java.util.UUID;

public record RoundResponse(
        UUID id,
        int roundNumber,
        RoundStatus status,
        GameState gameState,
        Integer playerOneRoundScore,
        Integer playerTwoRoundScore
) {
    static RoundResponse from(GameRound round, GameState gameState) {
        return new RoundResponse(
                round.getId(),
                round.getRoundNumber(),
                round.getStatus(),
                gameState,
                round.getPlayerOneRoundScore(),
                round.getPlayerTwoRoundScore()
        );
    }
}
