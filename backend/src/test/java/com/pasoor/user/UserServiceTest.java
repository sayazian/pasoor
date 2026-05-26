package com.pasoor.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserServiceTest {
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAllInBatch();
        userRepository.flush();
    }

    @Test
    void syncOAuthUserCreatesNewUser() {
        UserService userService = new UserService(userRepository);

        User user = userService.syncOAuthUser(new OAuthUserProfile(
                "google-123",
                "Sahar",
                "sahar@example.com",
                "https://example.com/avatar.png"
        ));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getGoogleSubject()).isEqualTo("google-123");
        assertThat(user.getName()).isEqualTo("Sahar");
        assertThat(user.getEmail()).isEqualTo("sahar@example.com");
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(user.getPreferredTheme()).isEqualTo(PreferredTheme.CLASSIC_GREEN_FELT);
        assertThat(user.isCaptureAnimationEnabled()).isTrue();
    }

    @Test
    void syncOAuthUserUpdatesExistingUserProfileFields() {
        UserService userService = new UserService(userRepository);
        userService.syncOAuthUser(new OAuthUserProfile("google-456", "Old Name", "old@example.com", null));

        User user = userService.syncOAuthUser(new OAuthUserProfile(
                "google-456",
                "New Name",
                "new@example.com",
                "https://example.com/new-avatar.png"
        ));

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(user.getName()).isEqualTo("Old Name");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/new-avatar.png");
    }

    @Test
    void syncOAuthUserKeepsEditedDisplayName() {
        UserService userService = new UserService(userRepository);
        User user = userService.syncOAuthUser(new OAuthUserProfile("google-789", "Google Name", "player@example.com", null));
        userService.updateProfile(user, new ProfileUpdateRequest("Table Name", PreferredTheme.PERSIAN_TILE, false));

        User synced = userService.syncOAuthUser(new OAuthUserProfile(
                "google-789",
                "Changed Google Name",
                "new-player@example.com",
                "https://example.com/avatar.png"
        ));

        assertThat(synced.getName()).isEqualTo("Table Name");
        assertThat(synced.getEmail()).isEqualTo("new-player@example.com");
        assertThat(synced.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(synced.getPreferredTheme()).isEqualTo(PreferredTheme.PERSIAN_TILE);
        assertThat(synced.isCaptureAnimationEnabled()).isFalse();
    }

    @Test
    void updateProfileTrimsNameAndStoresTheme() {
        UserService userService = new UserService(userRepository);
        User user = userService.syncOAuthUser(new OAuthUserProfile("google-101", "Sahar", "sahar@example.com", null));

        User updated = userService.updateProfile(user, new ProfileUpdateRequest(
                "  Card Player  ",
                PreferredTheme.DARK_CARD_ROOM,
                false
        ));

        assertThat(updated.getName()).isEqualTo("Card Player");
        assertThat(updated.getPreferredTheme()).isEqualTo(PreferredTheme.DARK_CARD_ROOM);
        assertThat(updated.isCaptureAnimationEnabled()).isFalse();
    }
}
