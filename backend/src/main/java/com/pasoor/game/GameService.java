package com.pasoor.game;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GameService {
    private GameState gameState;

    public GameService() {
        this.gameState = createNewGame();
    }

    public synchronized GameState newGame() {
        gameState = createNewGame();
        return gameState;
    }

    public synchronized GameState getGame() {
        return gameState;
    }

    public synchronized GameState deal() {
        if (gameState.getPhase() == GamePhase.FINISHED) {
            throw new IllegalStateException("Cannot deal after the game has finished.");
        }
        if (!gameState.getMyHand().isEmpty() || !gameState.getOpponentHand().isEmpty()) {
            throw new IllegalStateException("Deal only when both hands are empty.");
        }
        if (gameState.getDeck().isEmpty()) {
            gameState.setPhase(GamePhase.FINISHED);
            return gameState;
        }

        dealToHand(gameState.getMyHand(), 4);
        dealToHand(gameState.getOpponentHand(), 4);

        if (!gameState.isInitialDealDone()) {
            dealToHand(gameState.getTableCards(), 4);
            gameState.setInitialDealDone(true);
        }

        gameState.setCurrentTurn(Player.ME);
        gameState.setPhase(GamePhase.PLAYING);
        return gameState;
    }

    public synchronized GameState playCard(PlayCardRequest request) {
        if (gameState.getPhase() != GamePhase.PLAYING) {
            throw new IllegalStateException("Deal cards before playing.");
        }
        if (request.player() != gameState.getCurrentTurn()) {
            throw new IllegalStateException("It is not " + request.player() + "'s turn.");
        }

        List<Card> hand = handFor(request.player());
        Card card = removeCard(hand, request.cardId());
        gameState.getTableCards().add(card);

        if (gameState.getMyHand().isEmpty() && gameState.getOpponentHand().isEmpty() && gameState.getDeck().isEmpty()) {
            gameState.setPhase(GamePhase.FINISHED);
        } else {
            gameState.setCurrentTurn(nextPlayer(request.player()));
        }

        return gameState;
    }

    public synchronized GameState collectCards(CollectCardsRequest request) {
        if (request.cardIds() == null || request.cardIds().isEmpty()) {
            throw new IllegalArgumentException("Select at least one table card to collect.");
        }

        List<Card> collected = new ArrayList<>();
        for (String cardId : request.cardIds()) {
            collected.add(removeCard(gameState.getTableCards(), cardId));
        }

        if (request.player() == Player.ME) {
            gameState.getMyCollectedPile().addAll(collected);
        } else {
            gameState.getOpponentCollectedPile().addAll(collected);
        }

        return gameState;
    }

    private GameState createNewGame() {
        GameState state = new GameState();
        List<Card> deck = new ArrayList<>();

        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                deck.add(new Card(suit.name() + "-" + rank.name(), suit, rank, rank.getValue()));
            }
        }

        Collections.shuffle(deck);
        state.setDeck(deck);
        state.setPhase(GamePhase.NEW);
        state.setCurrentTurn(Player.ME);
        return state;
    }

    private void dealToHand(List<Card> destination, int count) {
        for (int index = 0; index < count && !gameState.getDeck().isEmpty(); index++) {
            destination.add(gameState.getDeck().removeFirst());
        }
    }

    private List<Card> handFor(Player player) {
        return player == Player.ME ? gameState.getMyHand() : gameState.getOpponentHand();
    }

    private Card removeCard(List<Card> cards, String cardId) {
        return cards.stream()
                .filter(card -> card.id().equals(cardId))
                .findFirst()
                .map(card -> {
                    cards.remove(card);
                    return card;
                })
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
    }

    private Player nextPlayer(Player player) {
        return player == Player.ME ? Player.OPPONENT : Player.ME;
    }
}
