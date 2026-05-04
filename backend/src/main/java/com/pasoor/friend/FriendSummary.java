package com.pasoor.friend;

import com.pasoor.user.User;

import java.util.UUID;

public record FriendSummary(UUID id, String name, String email, String avatarUrl) {
    public static FriendSummary from(User user) {
        return new FriendSummary(user.getId(), user.getName(), user.getEmail(), user.getAvatarUrl());
    }
}
