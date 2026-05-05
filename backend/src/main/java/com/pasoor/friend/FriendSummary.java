package com.pasoor.friend;

import com.pasoor.user.User;

import java.time.Instant;
import java.util.UUID;

public record FriendSummary(UUID id, String name, String email, String avatarUrl, boolean online) {
    public static FriendSummary from(User user) {
        return from(user, Instant.now());
    }

    public static FriendSummary from(User user, Instant now) {
        return new FriendSummary(user.getId(), user.getName(), user.getEmail(), user.getAvatarUrl(), user.isOnline(now));
    }
}
