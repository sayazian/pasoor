package com.pasoor.match;

import com.pasoor.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pasoor_match")
public class PasoorMatch {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_one_id", nullable = false)
    private User playerOne;

    @ManyToOne
    @JoinColumn(name = "player_two_id")
    private User playerTwo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    @Column(name = "player_one_total_score", nullable = false)
    private int playerOneTotalScore;

    @Column(name = "player_two_total_score", nullable = false)
    private int playerTwoTotalScore;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @ManyToOne
    @JoinColumn(name = "exited_by_id")
    private User exitedBy;

    @ManyToOne
    @JoinColumn(name = "rematch_id")
    private PasoorMatch rematch;

    @Enumerated(EnumType.STRING)
    @Column(name = "winner_side")
    private MatchWinner winnerSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "player_one_end_choice")
    private MatchEndChoice playerOneEndChoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "player_two_end_choice")
    private MatchEndChoice playerTwoEndChoice;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PasoorMatch() {
    }

    public PasoorMatch(User playerOne) {
        this.id = UUID.randomUUID();
        this.playerOne = playerOne;
        this.status = MatchStatus.ACTIVE;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getPlayerOne() {
        return playerOne;
    }

    public User getPlayerTwo() {
        return playerTwo;
    }

    public void setPlayerTwo(User playerTwo) {
        this.playerTwo = playerTwo;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public int getPlayerOneTotalScore() {
        return playerOneTotalScore;
    }

    public void setPlayerOneTotalScore(int playerOneTotalScore) {
        this.playerOneTotalScore = playerOneTotalScore;
    }

    public int getPlayerTwoTotalScore() {
        return playerTwoTotalScore;
    }

    public void setPlayerTwoTotalScore(int playerTwoTotalScore) {
        this.playerTwoTotalScore = playerTwoTotalScore;
    }

    public User getWinner() {
        return winner;
    }

    public void setWinner(User winner) {
        this.winner = winner;
    }

    public User getExitedBy() {
        return exitedBy;
    }

    public void setExitedBy(User exitedBy) {
        this.exitedBy = exitedBy;
    }

    public PasoorMatch getRematch() {
        return rematch;
    }

    public void setRematch(PasoorMatch rematch) {
        this.rematch = rematch;
    }

    public MatchWinner getWinnerSide() {
        return winnerSide;
    }

    public void setWinnerSide(MatchWinner winnerSide) {
        this.winnerSide = winnerSide;
    }

    public MatchEndChoice getPlayerOneEndChoice() {
        return playerOneEndChoice;
    }

    public void setPlayerOneEndChoice(MatchEndChoice playerOneEndChoice) {
        this.playerOneEndChoice = playerOneEndChoice;
    }

    public MatchEndChoice getPlayerTwoEndChoice() {
        return playerTwoEndChoice;
    }

    public void setPlayerTwoEndChoice(MatchEndChoice playerTwoEndChoice) {
        this.playerTwoEndChoice = playerTwoEndChoice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
