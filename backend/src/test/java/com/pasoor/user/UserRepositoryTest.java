package com.pasoor.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsUserWithDefaultThemeAndAuditFields() {
        User user = new User("google-123", "Sahar", "sahar@example.com", "https://example.com/avatar.png");

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPreferredTheme()).isEqualTo(PreferredTheme.CLASSIC_GREEN_FELT);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findsUserByGoogleSubjectAndEmail() {
        User user = userRepository.saveAndFlush(new User("google-456", "Player", "player@example.com", null));

        assertThat(userRepository.findByGoogleSubject("google-456")).contains(user);
        assertThat(userRepository.findByEmail("player@example.com")).contains(user);
    }

    @Test
    void updatesProfileFields() {
        User user = userRepository.saveAndFlush(new User("google-789", "Old Name", "profile@example.com", null));

        user.setName("New Name");
        user.setPreferredTheme(PreferredTheme.DARK_CARD_ROOM);
        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getName()).isEqualTo("New Name");
        assertThat(saved.getPreferredTheme()).isEqualTo(PreferredTheme.DARK_CARD_ROOM);
        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(saved.getCreatedAt());
    }
}
