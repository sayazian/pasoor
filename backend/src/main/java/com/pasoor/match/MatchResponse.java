package com.pasoor.match;

import com.pasoor.friend.FriendSummary;

import java.util.UUID;

public record MatchResponse(
        UUID id,
        MatchStatus status,
        FriendSummary playerOne,
        FriendSummary playerTwo,
        MatchPlayerSide viewerSide,
        int playerOneTotalScore,
        int playerTwoTotalScore,
        MatchWinner winnerSide,
        FriendSummary winner,
        RoundResponse currentRound
) {
    public static MatchResponse from(PasoorMatch match, MatchPlayerSide viewerSide, RoundResponse currentRound) {
        return new MatchResponse(
                match.getId(),
                match.getStatus(),
                FriendSummary.from(match.getPlayerOne()),
                match.getPlayerTwo() == null ? null : FriendSummary.from(match.getPlayerTwo()),
                viewerSide,
                match.getPlayerOneTotalScore(),
                match.getPlayerTwoTotalScore(),
                match.getWinnerSide(),
                match.getWinner() == null ? null : FriendSummary.from(match.getWinner()),
                currentRound
        );
    }
}
