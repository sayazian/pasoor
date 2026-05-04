package com.pasoor.game;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public GameState createGame() {
        return createNewGame();
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
        if (gameState.getPendingCaptureCard() != null) {
            throw new IllegalStateException("Finish the pending capture before dealing.");
        }
        if (gameState.getDeck().isEmpty()) {
            gameState.setPhase(GamePhase.FINISHED);
            return gameState;
        }

        dealToHand(gameState.getMyHand(), 4);
        dealToHand(gameState.getOpponentHand(), 4);

        if (!gameState.isInitialDealDone()) {
            dealInitialTableCards();
            gameState.setInitialDealDone(true);
        }

        gameState.setCurrentTurn(Player.ME);
        gameState.setPhase(GamePhase.PLAYING);
        return gameState;
    }

    public synchronized GameState deal(GameState state) {
        return withState(state, this::deal);
    }

    public synchronized GameState playCard(PlayCardRequest request) {
        if (gameState.getPhase() != GamePhase.PLAYING) {
            throw new IllegalStateException("Deal cards before playing.");
        }
        if (gameState.getPendingCaptureCard() != null) {
            throw new IllegalStateException("Finish the pending capture before playing another card.");
        }
        if (request.player() != gameState.getCurrentTurn()) {
            throw new IllegalStateException("It is not " + request.player() + "'s turn.");
        }

        List<Card> hand = handFor(request.player());
        Card card = removeCard(hand, request.cardId());
        gameState.getTableCards().add(card);

        if (hasAnyCapture(card)) {
            gameState.setPendingCapturePlayer(request.player());
            gameState.setPendingCaptureCard(card);
        } else {
            finishTurn(request.player());
        }

        return gameState;
    }

    public synchronized GameState playCard(GameState state, PlayCardRequest request) {
        return withState(state, () -> playCard(request));
    }

    public synchronized GameState captureCards(CaptureCardsRequest request) {
        Card playedCard = pendingCardFor(request.player());
        List<String> capturedTableCardIds = request.capturedTableCardIds() == null
                ? List.of()
                : request.capturedTableCardIds();
        List<Card> selectedTableCards = selectedTableCards(capturedTableCardIds);

        validateCapture(playedCard, selectedTableCards);
        removeCard(gameState.getTableCards(), playedCard.id());
        for (String cardId : capturedTableCardIds) {
            removeCard(gameState.getTableCards(), cardId);
        }

        List<Card> collected = collectedPileFor(request.player());
        collected.add(playedCard);
        collected.addAll(selectedTableCards);
        gameState.setLastCapturePlayer(request.player());

        if (gameState.getTableCards().isEmpty() && playedCard.rank() != Rank.JACK && !isLastRound()) {
            incrementSur(request.player());
        }

        finishTurn(request.player());
        return gameState;
    }

    public synchronized GameState captureCards(GameState state, CaptureCardsRequest request) {
        return withState(state, () -> captureCards(request));
    }

    private GameState withState(GameState state, java.util.function.Supplier<GameState> action) {
        GameState previousState = gameState;
        gameState = state;
        try {
            return action.get();
        } finally {
            gameState = previousState;
        }
    }

    private void finishTurn(Player player) {
        gameState.setPendingCapturePlayer(null);
        gameState.setPendingCaptureCard(null);
        if (gameState.getMyHand().isEmpty() && gameState.getOpponentHand().isEmpty() && gameState.getDeck().isEmpty()) {
            settleRemainingTableCards();
            gameState.setScore(calculateScore());
            gameState.setPhase(GamePhase.FINISHED);
        } else {
            gameState.setCurrentTurn(nextPlayer(player));
        }
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

    private void dealInitialTableCards() {
        List<Card> replacedJacks = new ArrayList<>();
        while (gameState.getTableCards().size() < 4 && !gameState.getDeck().isEmpty()) {
            Card card = gameState.getDeck().removeFirst();
            if (card.rank() == Rank.JACK) {
                replacedJacks.add(card);
            } else {
                gameState.getTableCards().add(card);
            }
        }
        gameState.getDeck().addAll(replacedJacks);
        Collections.shuffle(gameState.getDeck());
    }

    private List<Card> handFor(Player player) {
        return player == Player.ME ? gameState.getMyHand() : gameState.getOpponentHand();
    }

    private List<Card> collectedPileFor(Player player) {
        return player == Player.ME ? gameState.getMyCollectedPile() : gameState.getOpponentCollectedPile();
    }

    private Card pendingCardFor(Player player) {
        if (gameState.getPendingCaptureCard() == null || gameState.getPendingCapturePlayer() != player) {
            throw new IllegalStateException("There is no pending capture for " + player + ".");
        }

        return gameState.getPendingCaptureCard();
    }

    private Card findCard(List<Card> cards, String cardId) {
        return cards.stream()
                .filter(card -> card.id().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
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

    private List<Card> selectedTableCards(List<String> cardIds) {
        Set<String> uniqueIds = Set.copyOf(cardIds);
        if (uniqueIds.size() != cardIds.size()) {
            throw new IllegalArgumentException("A table card can only be selected once.");
        }

        return cardIds.stream()
                .map(cardId -> findCard(gameState.getTableCards(), cardId))
                .toList();
    }

    private boolean hasAnyCapture(Card playedCard) {
        List<Card> capturableTableCards = gameState.getTableCards().stream()
                .filter(card -> !card.id().equals(playedCard.id()))
                .toList();

        return switch (playedCard.rank()) {
            case JACK -> capturableTableCards.stream().anyMatch(this::isJackCaptureCard);
            case QUEEN -> capturableTableCards.stream().anyMatch(card -> card.rank() == Rank.QUEEN);
            case KING -> capturableTableCards.stream().anyMatch(card -> card.rank() == Rank.KING);
            default -> hasSubsetAddingTo(11 - playedCard.value(), capturableTableCards, 0);
        };
    }

    private void validateCapture(Card playedCard, List<Card> selectedTableCards) {
        if (selectedTableCards.isEmpty()) {
            throw new IllegalArgumentException("Select at least one table card to capture.");
        }

        switch (playedCard.rank()) {
            case JACK -> validateJackCapture(selectedTableCards);
            case QUEEN -> validateFaceCapture(selectedTableCards, Rank.QUEEN);
            case KING -> validateFaceCapture(selectedTableCards, Rank.KING);
            default -> validateNumberCapture(playedCard, selectedTableCards);
        }
    }

    private void validateJackCapture(List<Card> selectedTableCards) {
        Set<String> requiredIds = gameState.getTableCards().stream()
                .filter(card -> !card.id().equals(gameState.getPendingCaptureCard().id()))
                .filter(this::isJackCaptureCard)
                .map(Card::id)
                .collect(Collectors.toSet());
        Set<String> selectedIds = selectedTableCards.stream()
                .map(Card::id)
                .collect(Collectors.toSet());

        if (requiredIds.isEmpty() || !selectedIds.equals(requiredIds)) {
            throw new IllegalArgumentException("A Jack must capture every Jack, Ace, and number card on the table.");
        }
    }

    private void validateFaceCapture(List<Card> selectedTableCards, Rank rank) {
        if (selectedTableCards.size() != 1 || selectedTableCards.getFirst().rank() != rank) {
            throw new IllegalArgumentException(rank.name() + " can only capture one matching " + rank.name() + ".");
        }
    }

    private void validateNumberCapture(Card playedCard, List<Card> selectedTableCards) {
        int selectedValue = selectedTableCards.stream().mapToInt(Card::value).sum();
        if (playedCard.value() + selectedValue != 11 || selectedTableCards.stream().anyMatch(card -> !isNumberCaptureCard(card))) {
            throw new IllegalArgumentException("Number cards and Aces must capture table cards that sum with the played card to 11.");
        }
    }

    private boolean isJackCaptureCard(Card card) {
        return card.rank() == Rank.JACK || isNumberCaptureCard(card);
    }

    private boolean isNumberCaptureCard(Card card) {
        return card.rank() != Rank.JACK && card.rank() != Rank.QUEEN && card.rank() != Rank.KING;
    }

    private boolean hasSubsetAddingTo(int target, List<Card> cards, int index) {
        if (target == 0) {
            return true;
        }
        if (target < 0 || index >= cards.size()) {
            return false;
        }

        Card card = cards.get(index);
        if (!isNumberCaptureCard(card)) {
            return hasSubsetAddingTo(target, cards, index + 1);
        }

        return hasSubsetAddingTo(target - card.value(), cards, index + 1)
                || hasSubsetAddingTo(target, cards, index + 1);
    }

    private boolean isLastRound() {
        return gameState.getDeck().isEmpty();
    }

    private void incrementSur(Player player) {
        if (player == Player.ME) {
            gameState.setMySurCount(gameState.getMySurCount() + 1);
        } else {
            gameState.setOpponentSurCount(gameState.getOpponentSurCount() + 1);
        }
    }

    private void settleRemainingTableCards() {
        if (gameState.getTableCards().isEmpty() || gameState.getLastCapturePlayer() == null) {
            return;
        }

        collectedPileFor(gameState.getLastCapturePlayer()).addAll(gameState.getTableCards());
        gameState.getTableCards().clear();
    }

    private Score calculateScore() {
        int myCardPoints = cardPoints(gameState.getMyCollectedPile());
        int opponentCardPoints = cardPoints(gameState.getOpponentCollectedPile());
        int myClubCount = clubCount(gameState.getMyCollectedPile());
        int opponentClubCount = clubCount(gameState.getOpponentCollectedPile());
        int myMostClubsPoints = myClubCount > opponentClubCount ? 7 : 0;
        int opponentMostClubsPoints = opponentClubCount > myClubCount ? 7 : 0;
        int mySurPoints = gameState.getMySurCount() * 5;
        int opponentSurPoints = gameState.getOpponentSurCount() * 5;
        int myScore = myCardPoints + myMostClubsPoints + mySurPoints;
        int opponentScore = opponentCardPoints + opponentMostClubsPoints + opponentSurPoints;

        return new Score(
                myScore,
                opponentScore,
                myClubCount,
                opponentClubCount,
                mySurPoints,
                opponentSurPoints,
                myCardPoints + myMostClubsPoints,
                opponentCardPoints + opponentMostClubsPoints
        );
    }

    private int cardPoints(List<Card> cards) {
        return cards.stream()
                .mapToInt(card -> {
                    int points = 0;
                    if (card.rank() == Rank.ACE || card.rank() == Rank.JACK) {
                        points += 1;
                    }
                    if (card.suit() == Suit.DIAMONDS && card.rank() == Rank.TEN) {
                        points += 3;
                    }
                    if (card.suit() == Suit.CLUBS && card.rank() == Rank.TWO) {
                        points += 2;
                    }
                    return points;
                })
                .sum();
    }

    private int clubCount(List<Card> cards) {
        return (int) cards.stream()
                .filter(card -> card.suit() == Suit.CLUBS)
                .count();
    }

    private Player nextPlayer(Player player) {
        return player == Player.ME ? Player.OPPONENT : Player.ME;
    }
}
