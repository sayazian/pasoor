package com.pasoor.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(state.getTableCards()).noneMatch(card -> card.rank() == Rank.JACK);
    }

    @Test
    void firstDealPreservesConfiguredStartingPlayer() {
        GameService service = new GameService();
        GameState state = service.createGame(Player.OPPONENT);

        state = service.deal(state);

        assertThat(state.getCurrentTurn()).isEqualTo(Player.OPPONENT);
    }

    @Test
    void finishingAHandAutomaticallyDealsNextHandWhenDeckHasCards() {
        GameService service = new GameService();
        GameState state = service.createGame();
        state.setPhase(GamePhase.PLAYING);
        state.setInitialDealDone(true);
        state.setMyHand(new ArrayList<>(List.of(card(Suit.CLUBS, Rank.JACK))));
        state.setOpponentHand(new ArrayList<>(List.of(card(Suit.HEARTS, Rank.KING))));
        state.setDeck(new ArrayList<>(List.of(
                card(Suit.CLUBS, Rank.TWO),
                card(Suit.CLUBS, Rank.THREE),
                card(Suit.CLUBS, Rank.FOUR),
                card(Suit.CLUBS, Rank.FIVE),
                card(Suit.DIAMONDS, Rank.TWO),
                card(Suit.DIAMONDS, Rank.THREE),
                card(Suit.DIAMONDS, Rank.FOUR),
                card(Suit.DIAMONDS, Rank.FIVE)
        )));

        state = service.playCard(state, new PlayCardRequest(Player.ME, state.getMyHand().getFirst().id()));
        state = service.playCard(state, new PlayCardRequest(Player.OPPONENT, state.getOpponentHand().getFirst().id()));

        assertThat(state.getMyHand()).hasSize(4);
        assertThat(state.getOpponentHand()).hasSize(4);
        assertThat(state.getDeck()).isEmpty();
        assertThat(state.getCurrentTurn()).isEqualTo(Player.ME);
    }

    @Test
    void playCardDropsWhenNoCaptureIsAvailableAndAlternatesTurn() {
        GameService service = new GameService();
        GameState state = service.newGame();
        state.setDeck(new ArrayList<>(List.of(
                card(Suit.SPADES, Rank.KING),
                card(Suit.CLUBS, Rank.TWO),
                card(Suit.CLUBS, Rank.THREE),
                card(Suit.CLUBS, Rank.FOUR),
                card(Suit.DIAMONDS, Rank.TWO),
                card(Suit.DIAMONDS, Rank.THREE),
                card(Suit.DIAMONDS, Rank.FOUR),
                card(Suit.DIAMONDS, Rank.FIVE),
                card(Suit.HEARTS, Rank.ACE),
                card(Suit.HEARTS, Rank.TWO),
                card(Suit.HEARTS, Rank.THREE),
                card(Suit.HEARTS, Rank.FOUR)
        )));
        state = service.deal();
        Card card = state.getMyHand().getFirst();

        state = service.playCard(new PlayCardRequest(Player.ME, card.id()));

        assertThat(state.getMyHand()).hasSize(3);
        assertThat(state.getTableCards()).contains(card);
        assertThat(state.getPendingCaptureCard()).isNull();
        assertThat(state.getCurrentTurn()).isEqualTo(Player.OPPONENT);
    }

    @Test
    void cardWithCaptureStartsPendingCapture() {
        GameService service = new GameService();
        GameState state = service.newGame();
        state.setDeck(new ArrayList<>(List.of(
                card(Suit.SPADES, Rank.FIVE),
                card(Suit.CLUBS, Rank.TWO),
                card(Suit.CLUBS, Rank.THREE),
                card(Suit.CLUBS, Rank.FOUR),
                card(Suit.DIAMONDS, Rank.TWO),
                card(Suit.DIAMONDS, Rank.THREE),
                card(Suit.DIAMONDS, Rank.FOUR),
                card(Suit.DIAMONDS, Rank.FIVE),
                card(Suit.HEARTS, Rank.SIX),
                card(Suit.HEARTS, Rank.THREE),
                card(Suit.HEARTS, Rank.FOUR),
                card(Suit.HEARTS, Rank.FIVE)
        )));
        state = service.deal();
        Card playedCard = state.getMyHand().getFirst();

        state = service.playCard(new PlayCardRequest(Player.ME, playedCard.id()));

        assertThat(state.getPendingCaptureCard()).isEqualTo(playedCard);
        assertThat(state.getPendingCapturePlayer()).isEqualTo(Player.ME);
        assertThat(state.getCurrentTurn()).isEqualTo(Player.ME);
    }

    @Test
    void numberCaptureMovesPlayedAndCapturedCardsToCollectedPile() {
        GameService service = new GameService();
        GameState state = service.newGame();
        state.setDeck(new ArrayList<>(List.of(
                card(Suit.SPADES, Rank.FIVE),
                card(Suit.CLUBS, Rank.TWO),
                card(Suit.CLUBS, Rank.THREE),
                card(Suit.CLUBS, Rank.FOUR),
                card(Suit.DIAMONDS, Rank.TWO),
                card(Suit.DIAMONDS, Rank.THREE),
                card(Suit.DIAMONDS, Rank.FOUR),
                card(Suit.DIAMONDS, Rank.FIVE),
                card(Suit.HEARTS, Rank.TWO),
                card(Suit.HEARTS, Rank.FOUR),
                card(Suit.HEARTS, Rank.SEVEN),
                card(Suit.HEARTS, Rank.EIGHT)
        )));
        state = service.deal();
        Card playedCard = state.getMyHand().getFirst();
        Card capturedOne = state.getTableCards().get(0);
        Card capturedTwo = state.getTableCards().get(1);

        state = service.playCard(new PlayCardRequest(Player.ME, playedCard.id()));
        state = service.captureCards(new CaptureCardsRequest(Player.ME, List.of(capturedOne.id(), capturedTwo.id())));

        assertThat(state.getMyHand()).doesNotContain(playedCard);
        assertThat(state.getTableCards()).doesNotContain(capturedOne, capturedTwo);
        assertThat(state.getMyCollectedPile()).contains(playedCard, capturedOne, capturedTwo);
        assertThat(state.getCurrentTurn()).isEqualTo(Player.OPPONENT);
    }

    @Test
    void jackCapturesAllJacksAcesAndNumberCardsButDoesNotMakeSur() {
        GameService service = new GameService();
        GameState state = service.newGame();
        state.setDeck(new ArrayList<>(List.of(
                card(Suit.SPADES, Rank.JACK),
                card(Suit.CLUBS, Rank.TWO),
                card(Suit.CLUBS, Rank.THREE),
                card(Suit.CLUBS, Rank.FOUR),
                card(Suit.DIAMONDS, Rank.TWO),
                card(Suit.DIAMONDS, Rank.THREE),
                card(Suit.DIAMONDS, Rank.FOUR),
                card(Suit.DIAMONDS, Rank.FIVE),
                card(Suit.HEARTS, Rank.JACK),
                card(Suit.HEARTS, Rank.ACE),
                card(Suit.HEARTS, Rank.TEN),
                card(Suit.HEARTS, Rank.KING)
        )));
        state = service.deal();
        Card playedCard = state.getMyHand().getFirst();
        state.getTableCards().clear();
        state.getTableCards().addAll(List.of(
                card(Suit.HEARTS, Rank.JACK),
                card(Suit.HEARTS, Rank.ACE),
                card(Suit.HEARTS, Rank.TEN),
                card(Suit.HEARTS, Rank.KING)
        ));
        List<String> capturedIds = state.getTableCards().stream()
                .filter(card -> card.rank() != Rank.KING && card.rank() != Rank.QUEEN)
                .map(Card::id)
                .toList();

        state = service.playCard(new PlayCardRequest(Player.ME, playedCard.id()));
        state = service.captureCards(new CaptureCardsRequest(Player.ME, capturedIds));

        assertThat(state.getMyCollectedPile()).hasSize(4);
        assertThat(state.getTableCards()).extracting(Card::rank).containsExactly(Rank.KING);
        assertThat(state.getMySurCount()).isZero();
    }

    @Test
    void finalTurnGivesRemainingTableCardsToLastCapturerAndScoresGame() {
        GameService service = new GameService();
        GameState state = service.newGame();
        state.setDeck(new ArrayList<>());
        state.getMyHand().add(card(Suit.SPADES, Rank.KING));
        state.getOpponentHand().add(card(Suit.HEARTS, Rank.TWO));
        state.getTableCards().addAll(List.of(
                card(Suit.CLUBS, Rank.KING),
                card(Suit.CLUBS, Rank.TWO),
                card(Suit.DIAMONDS, Rank.TEN),
                card(Suit.SPADES, Rank.ACE)
        ));
        state.setPhase(GamePhase.PLAYING);
        state.setCurrentTurn(Player.ME);

        state = service.playCard(new PlayCardRequest(Player.ME, "SPADES-KING"));
        state = service.captureCards(new CaptureCardsRequest(Player.ME, List.of("CLUBS-KING")));
        state = service.playCard(new PlayCardRequest(Player.OPPONENT, "HEARTS-TWO"));

        assertThat(state.getPhase()).isEqualTo(GamePhase.FINISHED);
        assertThat(state.getTableCards()).isEmpty();
        assertThat(state.getMyCollectedPile()).extracting(Card::id)
                .contains("SPADES-KING", "CLUBS-KING", "CLUBS-TWO", "DIAMONDS-TEN", "SPADES-ACE", "HEARTS-TWO");
        assertThat(state.getScore()).isNotNull();
        assertThat(state.getScore().myScore()).isEqualTo(13);
        assertThat(state.getScore().opponentScore()).isZero();
    }

    private Card card(Suit suit, Rank rank) {
        return new Card(suit.name() + "-" + rank.name(), suit, rank, rank.getValue());
    }
}
