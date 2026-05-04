package com.pasoor.invite;

import com.pasoor.match.PasoorMatch;
import com.pasoor.user.User;
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
@Table(name = "game_invite")
public class GameInvite {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private PasoorMatch match;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameInviteStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected GameInvite() {
    }

    public GameInvite(PasoorMatch match, User sender, User recipient, String token) {
        this.id = UUID.randomUUID();
        this.match = match;
        this.sender = sender;
        this.recipient = recipient;
        this.recipientEmail = recipient.getEmail();
        this.token = token;
        this.status = GameInviteStatus.PENDING;
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

    public User getSender() {
        return sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getToken() {
        return token;
    }

    public GameInviteStatus getStatus() {
        return status;
    }

    public void setStatus(GameInviteStatus status) {
        this.status = status;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
