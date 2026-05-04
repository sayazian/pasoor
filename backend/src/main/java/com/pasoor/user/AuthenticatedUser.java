package com.pasoor.user;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String name,
        String email,
        String avatarUrl,
        PreferredTheme preferredTheme
) {
    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getPreferredTheme()
        );
    }
}

