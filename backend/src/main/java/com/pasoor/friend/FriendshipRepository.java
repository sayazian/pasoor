package com.pasoor.friend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    List<Friendship> findByRequesterIdOrRecipientId(UUID requesterId, UUID recipientId);

    @Query("""
            select friendship from Friendship friendship
            where (
                friendship.requester.id = :firstUserId and friendship.recipient.id = :secondUserId
            ) or (
                friendship.requester.id = :secondUserId and friendship.recipient.id = :firstUserId
            )
            """)
    List<Friendship> findBetweenUsers(UUID firstUserId, UUID secondUserId);

    @Query("""
            select friendship from Friendship friendship
            where friendship.id = :id
            and friendship.recipient.id = :recipientId
            """)
    Optional<Friendship> findIncomingRequest(UUID id, UUID recipientId);

}
