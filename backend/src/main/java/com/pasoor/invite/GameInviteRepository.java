package com.pasoor.invite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameInviteRepository extends JpaRepository<GameInvite, UUID> {
    Optional<GameInvite> findByToken(String token);

    List<GameInvite> findByRecipientIdAndStatusOrderByCreatedAtDesc(UUID recipientId, GameInviteStatus status);
}
