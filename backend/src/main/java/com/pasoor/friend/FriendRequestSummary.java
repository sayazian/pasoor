package com.pasoor.friend;

import java.util.UUID;

public record FriendRequestSummary(
        UUID id,
        FriendSummary requester,
        FriendSummary recipient,
        FriendshipStatus status,
        String message
) {
    static FriendRequestSummary from(Friendship friendship) {
        return new FriendRequestSummary(
                friendship.getId(),
                FriendSummary.from(friendship.getRequester()),
                FriendSummary.from(friendship.getRecipient()),
                friendship.getStatus(),
                friendship.getMessage()
        );
    }
}
