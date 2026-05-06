package com.pasoor.match;

import com.pasoor.friend.FriendSummary;

import java.util.List;
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
        FriendSummary exitedBy,
        MatchEndChoice playerOneEndChoice,
        MatchEndChoice playerTwoEndChoice,
        UUID rematchId,
        RoundResponse currentRound,
        RoundResponse lastCompletedRound,
        List<RoundResponse> completedRounds
) {
    public static MatchResponse from(
            PasoorMatch match,
            MatchPlayerSide viewerSide,
            RoundResponse currentRound,
            RoundResponse lastCompletedRound,
            List<RoundResponse> completedRounds
    ) {
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
                match.getExitedBy() == null ? null : FriendSummary.from(match.getExitedBy()),
                match.getPlayerOneEndChoice(),
                match.getPlayerTwoEndChoice(),
                match.getRematch() == null ? null : match.getRematch().getId(),
                currentRound,
                lastCompletedRound,
                completedRounds
        );
    }
}
