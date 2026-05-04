package com.pasoor.match;

import com.pasoor.friend.FriendSummary;

import java.util.UUID;

public record MatchResponse(
        UUID id,
        MatchStatus status,
        FriendSummary playerOne,
        FriendSummary playerTwo,
        int playerOneTotalScore,
        int playerTwoTotalScore,
        MatchWinner winnerSide,
        FriendSummary winner,
        RoundResponse currentRound
) {
    static MatchResponse from(PasoorMatch match, RoundResponse currentRound) {
        return new MatchResponse(
                match.getId(),
                match.getStatus(),
                FriendSummary.from(match.getPlayerOne()),
                match.getPlayerTwo() == null ? null : FriendSummary.from(match.getPlayerTwo()),
                match.getPlayerOneTotalScore(),
                match.getPlayerTwoTotalScore(),
                match.getWinnerSide(),
                match.getWinner() == null ? null : FriendSummary.from(match.getWinner()),
                currentRound
        );
    }
}
