package com.pasoor.security;

import com.pasoor.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsAndPersistsAuthenticatedOAuthUser() throws Exception {
        mockMvc.perform(get("/api/me")
                        .with(oauth2Login().attributes(attributes -> {
                            attributes.put("sub", "google-123");
                            attributes.put("name", "Sahar");
                            attributes.put("email", "sahar@example.com");
                            attributes.put("picture", "https://example.com/avatar.png");
                        })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sahar"))
                .andExpect(jsonPath("$.email").value("sahar@example.com"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.preferredTheme").value("CLASSIC_GREEN_FELT"));

        assertThat(userRepository.findByGoogleSubject("google-123")).isPresent();
    }

    @Test
    void gameEndpointsRemainPublicDuringPhaseTwo() throws Exception {
        mockMvc.perform(post("/api/game/new").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckCount").value(52));
    }

    @Test
    void logoutEndpointReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/logout").with(oauth2Login()))
                .andExpect(status().isNoContent());
    }
}
