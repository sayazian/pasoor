package com.pasoor.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceTest {
    @Test
    void newGameStartsWithFullDeckAndEmptyBoard() {
        GameService service = new GameService();
        GameState state = service.newGame();

        assertThat(state.getDeckCount()).isEqualTo(52);
        assertThat(state.getMyHand()).isEmpty();
        assertThat(state.getOpponentHand()).isEmpty();
        assertThat(state.getTableCards()).isEmpty();
        assertThat(state.getPhase()).isEqualTo(GamePhase.NEW);
    }

    @Test
    void firstDealDealsHandsAndTable() {
        GameService service = new GameService();
        GameState state = service.deal();

        assertThat(state.getDeckCount()).isEqualTo(40);
        assertThat(state.getMyHand()).hasSize(4);
        assertThat(state.getOpponentHand()).hasSize(4);
        assertThat(state.getTableCards()).hasSize(4);
        assertThat(state.getPhase()).isEqualTo(GamePhase.PLAYING);
    }

    @Test
    void playCardMovesCardToTableAndAlternatesTurn() {
        GameService service = new GameService();
        GameState state = service.deal();
        Card card = state.getMyHand().getFirst();

        state = service.playCard(new PlayCardRequest(Player.ME, card.id()));

        assertThat(state.getMyHand()).hasSize(3);
        assertThat(state.getTableCards()).contains(card);
        assertThat(state.getCurrentTurn()).isEqualTo(Player.OPPONENT);
    }

    @Test
    void collectCardsMovesSelectedCardsToChosenPile() {
        GameService service = new GameService();
        GameState state = service.deal();
        Card first = state.getTableCards().get(0);
        Card second = state.getTableCards().get(1);

        state = service.collectCards(new CollectCardsRequest(Player.OPPONENT, List.of(first.id(), second.id())));

        assertThat(state.getTableCards()).doesNotContain(first, second);
        assertThat(state.getOpponentCollectedPile()).contains(first, second);
    }
}
