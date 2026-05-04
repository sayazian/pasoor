package com.pasoor.game;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class GameServicePerformanceTest {
    @Test
    void repeatedNewGamesAndDealsStayFast() {
        assertTimeout(Duration.ofSeconds(1), () -> {
            GameService service = new GameService();

            for (int index = 0; index < 1_000; index++) {
                GameState state = service.newGame();
                assertThat(state.getDeckCount()).isEqualTo(52);

                state = service.deal();
                assertThat(state.getDeckCount()).isEqualTo(40);
                assertThat(state.getMyHand()).hasSize(4);
                assertThat(state.getOpponentHand()).hasSize(4);
                assertThat(state.getTableCards()).hasSize(4);
            }
        });
    }

    @Test
    void captureSearchWithLargeTableStaysFast() {
        assertTimeout(Duration.ofMillis(250), () -> {
            GameService service = new GameService();
            GameState state = service.newGame();
            state.setDeck(new ArrayList<>());
            state.setPhase(GamePhase.PLAYING);
            state.setCurrentTurn(Player.ME);
            state.getMyHand().add(card(Suit.SPADES, Rank.ACE));

            for (Suit suit : Suit.values()) {
                for (Rank rank : List.of(Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN)) {
                    state.getTableCards().add(card(suit, rank));
                }
            }

            state = service.playCard(new PlayCardRequest(Player.ME, "SPADES-ACE"));

            assertThat(state.getPendingCaptureCard()).isNotNull();
            assertThat(state.getPendingCapturePlayer()).isEqualTo(Player.ME);
        });
    }

    @Test
    void finalScoringWithFullCollectedPilesStaysFast() {
        assertTimeout(Duration.ofMillis(250), () -> {
            GameService service = new GameService();
            GameState state = service.newGame();
            state.setDeck(new ArrayList<>());
            state.setPhase(GamePhase.PLAYING);
            state.setCurrentTurn(Player.ME);
            state.setLastCapturePlayer(Player.ME);

            List<Card> deck = fullDeck();
            state.getMyCollectedPile().addAll(deck.subList(0, 24));
            state.getOpponentCollectedPile().addAll(deck.subList(24, 48));
            state.getTableCards().addAll(deck.subList(48, 50));
            state.getMyHand().add(deck.get(50));
            state.getOpponentHand().add(deck.get(51));

            state = service.playCard(new PlayCardRequest(Player.ME, deck.get(50).id()));
            if (state.getPendingCaptureCard() != null) {
                state = service.captureCards(new CaptureCardsRequest(Player.ME, capturableIdsForPendingCapture(state)));
            }
            state = service.playCard(new PlayCardRequest(Player.OPPONENT, deck.get(51).id()));
            if (state.getPendingCaptureCard() != null) {
                state = service.captureCards(new CaptureCardsRequest(Player.OPPONENT, capturableIdsForPendingCapture(state)));
            }

            assertThat(state.getPhase()).isEqualTo(GamePhase.FINISHED);
            assertThat(state.getScore()).isNotNull();
        });
    }

    @Test
    void deterministicFullGameSimulationStaysFast() {
        assertTimeout(Duration.ofSeconds(1), () -> {
            GameService service = new GameService();
            GameState state = service.deal();

            while (state.getPhase() != GamePhase.FINISHED) {
                if (state.getMyHand().isEmpty() && state.getOpponentHand().isEmpty()) {
                    state = service.deal();
                    continue;
                }

                Player player = state.getCurrentTurn();
                Card card = handFor(state, player).getFirst();
                state = service.playCard(new PlayCardRequest(player, card.id()));

                if (state.getPendingCaptureCard() != null) {
                    state = service.captureCards(new CaptureCardsRequest(player, capturableIdsForPendingCapture(state)));
                }
            }

            assertThat(state.getScore()).isNotNull();
        });
    }

    private List<Card> handFor(GameState state, Player player) {
        return player == Player.ME ? state.getMyHand() : state.getOpponentHand();
    }

    private List<String> capturableIdsForPendingCapture(GameState state) {
        Card playedCard = state.getPendingCaptureCard();
        List<Card> tableCards = state.getTableCards().stream()
                .filter(card -> !card.id().equals(playedCard.id()))
                .toList();

        return switch (playedCard.rank()) {
            case JACK -> tableCards.stream()
                    .filter(card -> card.rank() != Rank.KING && card.rank() != Rank.QUEEN)
                    .map(Card::id)
                    .toList();
            case QUEEN -> List.of(firstCardWithRank(tableCards, Rank.QUEEN).id());
            case KING -> List.of(firstCardWithRank(tableCards, Rank.KING).id());
            default -> numberCaptureIds(11 - playedCard.value(), tableCards, 0, new ArrayList<>());
        };
    }

    private Card firstCardWithRank(List<Card> cards, Rank rank) {
        return cards.stream()
                .filter(card -> card.rank() == rank)
                .findFirst()
                .orElseThrow();
    }

    private List<String> numberCaptureIds(int target, List<Card> cards, int index, List<String> selectedIds) {
        if (target == 0) {
            return selectedIds;
        }
        if (target < 0 || index >= cards.size()) {
            return List.of();
        }

        Card card = cards.get(index);
        if (!isNumberCaptureCard(card)) {
            return numberCaptureIds(target, cards, index + 1, selectedIds);
        }

        List<String> withCard = new ArrayList<>(selectedIds);
        withCard.add(card.id());
        List<String> included = numberCaptureIds(target - card.value(), cards, index + 1, withCard);
        if (!included.isEmpty()) {
            return included;
        }

        return numberCaptureIds(target, cards, index + 1, selectedIds);
    }

    private boolean isNumberCaptureCard(Card card) {
        return card.rank() != Rank.JACK && card.rank() != Rank.QUEEN && card.rank() != Rank.KING;
    }

    private List<Card> fullDeck() {
        List<Card> cards = new ArrayList<>();

        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(card(suit, rank));
            }
        }

        return cards;
    }

    private Card card(Suit suit, Rank rank) {
        return new Card(suit.name() + "-" + rank.name(), suit, rank, rank.getValue());
    }
}
