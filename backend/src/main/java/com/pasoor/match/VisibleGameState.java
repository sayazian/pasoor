package com.pasoor.match;

import com.pasoor.game.Card;
import com.pasoor.game.GamePhase;
import com.pasoor.game.GameState;
import com.pasoor.game.Player;
import com.pasoor.game.Score;

import java.util.List;

public record VisibleGameState(
        int deckCount,
        List<Card> myHand,
        int opponentHandCount,
        List<Card> opponentHand,
        List<Card> tableCards,
        List<Card> myCollectedPile,
        List<Card> opponentCollectedPile,
        Player currentTurn,
        GamePhase phase,
        boolean initialDealDone,
        int mySurCount,
        int opponentSurCount,
        Player pendingCapturePlayer,
        Card pendingCaptureCard,
        Player lastCapturePlayer,
        Score score
) {
    static VisibleGameState from(GameState state, Player viewer) {
        boolean playerOneView = viewer == Player.ME;
        return new VisibleGameState(
                state.getDeckCount(),
                playerOneView ? state.getMyHand() : state.getOpponentHand(),
                playerOneView ? state.getOpponentHand().size() : state.getMyHand().size(),
                List.of(),
                state.getTableCards(),
                playerOneView ? state.getMyCollectedPile() : state.getOpponentCollectedPile(),
                playerOneView ? state.getOpponentCollectedPile() : state.getMyCollectedPile(),
                visiblePlayer(state.getCurrentTurn(), viewer),
                state.getPhase(),
                state.isInitialDealDone(),
                playerOneView ? state.getMySurCount() : state.getOpponentSurCount(),
                playerOneView ? state.getOpponentSurCount() : state.getMySurCount(),
                visiblePlayer(state.getPendingCapturePlayer(), viewer),
                state.getPendingCaptureCard(),
                visiblePlayer(state.getLastCapturePlayer(), viewer),
                visibleScore(state.getScore(), viewer)
        );
    }

    private static Player visiblePlayer(Player player, Player viewer) {
        if (player == null) {
            return null;
        }

        return player == viewer ? Player.ME : Player.OPPONENT;
    }

    private static Score visibleScore(Score score, Player viewer) {
        if (score == null || viewer == Player.ME) {
            return score;
        }

        return new Score(
                score.opponentScore(),
                score.myScore(),
                score.opponentClubCount(),
                score.myClubCount(),
                score.opponentSurPoints(),
                score.mySurPoints(),
                score.opponentCardPoints(),
                score.myCardPoints()
        );
    }
}
