package com.pasoor.user;

public record OAuthUserProfile(
        String googleSubject,
        String name,
        String email,
        String avatarUrl
) {
}

