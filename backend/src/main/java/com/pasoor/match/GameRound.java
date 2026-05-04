package com.pasoor.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_round")
public class GameRound {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private PasoorMatch match;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStatus status;

    @Column(name = "game_state_json", nullable = false)
    private String gameStateJson;

    @Column(name = "player_one_round_score")
    private Integer playerOneRoundScore;

    @Column(name = "player_two_round_score")
    private Integer playerTwoRoundScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected GameRound() {
    }

    public GameRound(PasoorMatch match, int roundNumber, String gameStateJson) {
        this.id = UUID.randomUUID();
        this.match = match;
        this.roundNumber = roundNumber;
        this.status = RoundStatus.ACTIVE;
        this.gameStateJson = gameStateJson;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public PasoorMatch getMatch() {
        return match;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public RoundStatus getStatus() {
        return status;
    }

    public void setStatus(RoundStatus status) {
        this.status = status;
    }

    public String getGameStateJson() {
        return gameStateJson;
    }

    public void setGameStateJson(String gameStateJson) {
        this.gameStateJson = gameStateJson;
    }

    public Integer getPlayerOneRoundScore() {
        return playerOneRoundScore;
    }

    public void setPlayerOneRoundScore(Integer playerOneRoundScore) {
        this.playerOneRoundScore = playerOneRoundScore;
    }

    public Integer getPlayerTwoRoundScore() {
        return playerTwoRoundScore;
    }

    public void setPlayerTwoRoundScore(Integer playerTwoRoundScore) {
        this.playerTwoRoundScore = playerTwoRoundScore;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
