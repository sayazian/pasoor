package com.pasoor.game;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private List<Card> deck = new ArrayList<>();
    private List<Card> myHand = new ArrayList<>();
    private List<Card> opponentHand = new ArrayList<>();
    private List<Card> tableCards = new ArrayList<>();
    private List<Card> myCollectedPile = new ArrayList<>();
    private List<Card> opponentCollectedPile = new ArrayList<>();
    private Player currentTurn = Player.ME;
    private GamePhase phase = GamePhase.NEW;
    private boolean initialDealDone;
    private int mySurCount;
    private int opponentSurCount;
    private Player pendingCapturePlayer;
    private Card pendingCaptureCard;
    private Player lastCapturePlayer;
    private Score score;

    public int getDeckCount() {
        return deck.size();
    }

    public List<Card> getDeck() {
        return deck;
    }

    public void setDeck(List<Card> deck) {
        this.deck = deck;
    }

    public List<Card> getMyHand() {
        return myHand;
    }

    public void setMyHand(List<Card> myHand) {
        this.myHand = myHand;
    }

    public List<Card> getOpponentHand() {
        return opponentHand;
    }

    public void setOpponentHand(List<Card> opponentHand) {
        this.opponentHand = opponentHand;
    }

    public List<Card> getTableCards() {
        return tableCards;
    }

    public void setTableCards(List<Card> tableCards) {
        this.tableCards = tableCards;
    }

    public List<Card> getMyCollectedPile() {
        return myCollectedPile;
    }

    public void setMyCollectedPile(List<Card> myCollectedPile) {
        this.myCollectedPile = myCollectedPile;
    }

    public List<Card> getOpponentCollectedPile() {
        return opponentCollectedPile;
    }

    public void setOpponentCollectedPile(List<Card> opponentCollectedPile) {
        this.opponentCollectedPile = opponentCollectedPile;
    }

    public Player getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(Player currentTurn) {
        this.currentTurn = currentTurn;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public boolean isInitialDealDone() {
        return initialDealDone;
    }

    public void setInitialDealDone(boolean initialDealDone) {
        this.initialDealDone = initialDealDone;
    }

    public int getMySurCount() {
        return mySurCount;
    }

    public void setMySurCount(int mySurCount) {
        this.mySurCount = mySurCount;
    }

    public int getOpponentSurCount() {
        return opponentSurCount;
    }

    public void setOpponentSurCount(int opponentSurCount) {
        this.opponentSurCount = opponentSurCount;
    }

    public Player getPendingCapturePlayer() {
        return pendingCapturePlayer;
    }

    public void setPendingCapturePlayer(Player pendingCapturePlayer) {
        this.pendingCapturePlayer = pendingCapturePlayer;
    }

    public Card getPendingCaptureCard() {
        return pendingCaptureCard;
    }

    public void setPendingCaptureCard(Card pendingCaptureCard) {
        this.pendingCaptureCard = pendingCaptureCard;
    }

    public Player getLastCapturePlayer() {
        return lastCapturePlayer;
    }

    public void setLastCapturePlayer(Player lastCapturePlayer) {
        this.lastCapturePlayer = lastCapturePlayer;
    }

    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
    }
}
