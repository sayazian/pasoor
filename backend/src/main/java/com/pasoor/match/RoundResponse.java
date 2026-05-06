package com.pasoor.match;

import java.util.UUID;

public record RoundResponse(
        UUID id,
        int roundNumber,
        RoundStatus status,
        VisibleGameState gameState,
        Integer playerOneRoundScore,
        Integer playerTwoRoundScore,
        boolean playerOneAcknowledged,
        boolean playerTwoAcknowledged
) {
    static RoundResponse from(GameRound round, VisibleGameState gameState) {
        return new RoundResponse(
                round.getId(),
                round.getRoundNumber(),
                round.getStatus(),
                gameState,
                round.getPlayerOneRoundScore(),
                round.getPlayerTwoRoundScore(),
                round.getPlayerOneAcknowledgedAt() != null,
                round.getPlayerTwoAcknowledgedAt() != null
        );
    }
}
