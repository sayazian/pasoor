package com.pasoor.user;

public record ProfileUpdateRequest(
        String name,
        PreferredTheme preferredTheme,
        Boolean captureAnimationEnabled
) {
}
