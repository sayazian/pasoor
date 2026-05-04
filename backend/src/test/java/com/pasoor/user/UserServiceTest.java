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
        userRepository.deleteAll();
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
        assertThat(user.getName()).isEqualTo("New Name");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/new-avatar.png");
    }
}
