package com.pasoor.friend;

import java.util.List;

public record FriendsResponse(
        List<FriendSummary> friends,
        List<FriendRequestSummary> incomingRequests,
        List<FriendRequestSummary> outgoingRequests
) {
}
