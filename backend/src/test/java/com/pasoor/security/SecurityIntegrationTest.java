package com.pasoor.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasoor.friend.Friendship;
import com.pasoor.friend.FriendshipRepository;
import com.pasoor.invite.GameInviteRepository;
import com.pasoor.match.GameRoundRepository;
import com.pasoor.match.MatchRepository;
import com.pasoor.user.User;
import com.pasoor.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private GameInviteRepository inviteRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private GameRoundRepository roundRepository;

    @BeforeEach
    void clearUsers() {
        inviteRepository.deleteAllInBatch();
        inviteRepository.flush();
        roundRepository.deleteAllInBatch();
        roundRepository.flush();
        matchRepository.deleteAllInBatch();
        matchRepository.flush();
        friendshipRepository.deleteAllInBatch();
        friendshipRepository.flush();
        userRepository.deleteAllInBatch();
        userRepository.flush();
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileUpdateRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Sahar","preferredTheme":"MODERN_LIGHT_TABLE"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void friendsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void matchesRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/matches"))
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
    void createMatchPersistsMatchAndFirstRound() throws Exception {
        mockMvc.perform(post("/api/matches")
                        .with(oauthUser("player-google", "Player", "player@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.playerOne.email").value("player@example.com"))
                .andExpect(jsonPath("$.playerOneTotalScore").value(0))
                .andExpect(jsonPath("$.playerTwoTotalScore").value(0))
                .andExpect(jsonPath("$.currentRound.roundNumber").value(1))
                .andExpect(jsonPath("$.currentRound.gameState.deckCount").value(52));

        assertThat(matchRepository.count()).isEqualTo(1);
        assertThat(roundRepository.count()).isEqualTo(1);
    }

    @Test
    void matchDealUpdatesPersistedRound() throws Exception {
        String createResponse = mockMvc.perform(post("/api/matches")
                        .with(oauthUser("player-google", "Player", "player@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode createJson = objectMapper.readTree(createResponse);
        String matchId = createJson.get("id").asText();
        String roundId = createJson.get("currentRound").get("id").asText();

        mockMvc.perform(post("/api/matches/{matchId}/rounds/{roundId}/deal", matchId, roundId)
                        .with(oauthUser("player-google", "Player", "player@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRound.id").value(roundId))
                .andExpect(jsonPath("$.currentRound.gameState.deckCount").value(40))
                .andExpect(jsonPath("$.currentRound.gameState.myHand").isArray())
                .andExpect(jsonPath("$.currentRound.gameState.myHand.length()").value(4))
                .andExpect(jsonPath("$.currentRound.gameState.tableCards.length()").value(4));

        mockMvc.perform(get("/api/matches/{matchId}", matchId)
                        .with(oauthUser("player-google", "Player", "player@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRound.gameState.deckCount").value(40));
    }

    @Test
    void exitMatchMarksMatchAbandoned() throws Exception {
        String createResponse = mockMvc.perform(post("/api/matches")
                        .with(oauthUser("player-google", "Player", "player@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String matchId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(post("/api/matches/{matchId}/exit", matchId)
                        .with(oauthUser("player-google", "Player", "player@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABANDONED"));
    }

    @Test
    void inviteAcceptedFriendCreatesWaitingMatchInvite() throws Exception {
        User sender = userRepository.saveAndFlush(new User("sender-google", "Sender", "sender@example.com", null));
        User recipient = userRepository.saveAndFlush(new User("recipient-google", "Recipient", "recipient@example.com", null));
        Friendship friendship = friendshipRepository.saveAndFlush(new Friendship(sender, recipient, "Want to play?"));
        mockMvc.perform(post("/api/friends/requests/{id}/accept", friendship.getId())
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk());
        String createResponse = mockMvc.perform(post("/api/matches")
                        .with(oauthUser("sender-google", "Sender", "sender@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String matchId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(post("/api/matches/{matchId}/invite", matchId)
                        .with(oauthUser("sender-google", "Sender", "sender@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"recipient@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.recipient.email").value("recipient@example.com"))
                .andExpect(jsonPath("$.inviteLink").value(org.hamcrest.Matchers.containsString("/invite/")))
                .andExpect(jsonPath("$.match.status").value("WAITING"));
    }

    @Test
    void inviteRequiresAcceptedFriend() throws Exception {
        userRepository.saveAndFlush(new User("recipient-google", "Recipient", "recipient@example.com", null));
        String createResponse = mockMvc.perform(post("/api/matches")
                        .with(oauthUser("sender-google", "Sender", "sender@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String matchId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(post("/api/matches/{matchId}/invite", matchId)
                        .with(oauthUser("sender-google", "Sender", "sender@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"recipient@example.com"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void invitedFriendAcceptsInviteAndActivatesMatch() throws Exception {
        User sender = userRepository.saveAndFlush(new User("sender-google", "Sender", "sender@example.com", null));
        User recipient = userRepository.saveAndFlush(new User("recipient-google", "Recipient", "recipient@example.com", null));
        Friendship friendship = friendshipRepository.saveAndFlush(new Friendship(sender, recipient, "Want to play?"));
        mockMvc.perform(post("/api/friends/requests/{id}/accept", friendship.getId())
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk());
        String createResponse = mockMvc.perform(post("/api/matches")
                        .with(oauthUser("sender-google", "Sender", "sender@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String matchId = objectMapper.readTree(createResponse).get("id").asText();
        String inviteResponse = mockMvc.perform(post("/api/matches/{matchId}/invite", matchId)
                        .with(oauthUser("sender-google", "Sender", "sender@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"recipient@example.com"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(inviteResponse).get("token").asText();

        mockMvc.perform(get("/api/invites/{token}", token)
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match.status").value("WAITING"));

        mockMvc.perform(post("/api/invites/{token}/accept", token)
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.match.status").value("ACTIVE"))
                .andExpect(jsonPath("$.match.playerTwo.email").value("recipient@example.com"));
    }

    @Test
    void logoutEndpointReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/logout").with(oauth2Login()))
                .andExpect(status().isNoContent());
    }

    @Test
    void profileUpdatePersistsNameAndThemeButKeepsEmailFromOAuth() throws Exception {
        mockMvc.perform(patch("/api/me/profile")
                        .with(oauth2Login().attributes(attributes -> {
                            attributes.put("sub", "google-789");
                            attributes.put("name", "Google Name");
                            attributes.put("email", "sahar@example.com");
                            attributes.put("picture", "https://example.com/avatar.png");
                        }))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Card Player","preferredTheme":"DARK_CARD_ROOM"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Card Player"))
                .andExpect(jsonPath("$.email").value("sahar@example.com"))
                .andExpect(jsonPath("$.preferredTheme").value("DARK_CARD_ROOM"));

        assertThat(userRepository.findByGoogleSubject("google-789"))
                .hasValueSatisfying(user -> {
                    assertThat(user.getName()).isEqualTo("Card Player");
                    assertThat(user.getEmail()).isEqualTo("sahar@example.com");
                });
    }

    @Test
    void profileUpdateRejectsBlankName() throws Exception {
        mockMvc.perform(patch("/api/me/profile")
                        .with(oauth2Login().attributes(attributes -> {
                            attributes.put("sub", "google-999");
                            attributes.put("name", "Google Name");
                            attributes.put("email", "google@example.com");
                        }))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   ","preferredTheme":"CLASSIC_GREEN_FELT"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void friendRequestCreatesOutgoingAndIncomingPendingViews() throws Exception {
        User recipient = userRepository.saveAndFlush(new User(
                "recipient-google",
                "Recipient",
                "friend@example.com",
                null
        ));

        mockMvc.perform(post("/api/friends/requests")
                        .with(oauthUser("requester-google", "Requester", "requester@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"friend@example.com","message":"Want to play Pasoor?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outgoingRequests[0].recipient.email").value("friend@example.com"))
                .andExpect(jsonPath("$.outgoingRequests[0].message").value("Want to play Pasoor?"))
                .andExpect(jsonPath("$.incomingRequests").isEmpty())
                .andExpect(jsonPath("$.friends").isEmpty());

        mockMvc.perform(get("/api/friends")
                        .with(oauthUser("recipient-google", "Recipient", recipient.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomingRequests[0].requester.email").value("requester@example.com"))
                .andExpect(jsonPath("$.outgoingRequests").isEmpty());
    }

    @Test
    void friendRequestRequiresExistingRecipient() throws Exception {
        mockMvc.perform(post("/api/friends/requests")
                        .with(oauthUser("requester-google", "Requester", "requester@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@example.com","message":"Want to play?"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void friendRequestCannotTargetSelf() throws Exception {
        mockMvc.perform(post("/api/friends/requests")
                        .with(oauthUser("requester-google", "Requester", "requester@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"requester@example.com","message":"Want to play?"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicatePendingFriendRequestIsBlocked() throws Exception {
        userRepository.saveAndFlush(new User("recipient-google", "Recipient", "friend@example.com", null));

        mockMvc.perform(post("/api/friends/requests")
                        .with(oauthUser("requester-google", "Requester", "requester@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"friend@example.com","message":"Want to play?"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/friends/requests")
                        .with(oauthUser("requester-google", "Requester", "requester@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"friend@example.com","message":"Still want to play?"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void acceptedRequestShowsFriendForBothUsers() throws Exception {
        User requester = userRepository.saveAndFlush(new User(
                "requester-google",
                "Requester",
                "requester@example.com",
                null
        ));
        User recipient = userRepository.saveAndFlush(new User(
                "recipient-google",
                "Recipient",
                "recipient@example.com",
                null
        ));
        Friendship friendship = friendshipRepository.saveAndFlush(new Friendship(requester, recipient, "Want to play?"));

        mockMvc.perform(post("/api/friends/requests/{id}/accept", friendship.getId())
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends[0].email").value("requester@example.com"))
                .andExpect(jsonPath("$.incomingRequests").isEmpty());

        mockMvc.perform(get("/api/friends")
                        .with(oauthUser("requester-google", "Requester", "requester@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends[0].email").value("recipient@example.com"))
                .andExpect(jsonPath("$.outgoingRequests").isEmpty());
    }

    @Test
    void rejectedRequestLeavesNoPendingOrAcceptedFriendship() throws Exception {
        User requester = userRepository.saveAndFlush(new User(
                "requester-google",
                "Requester",
                "requester@example.com",
                null
        ));
        User recipient = userRepository.saveAndFlush(new User(
                "recipient-google",
                "Recipient",
                "recipient@example.com",
                null
        ));
        Friendship friendship = friendshipRepository.saveAndFlush(new Friendship(requester, recipient, "Want to play?"));

        mockMvc.perform(post("/api/friends/requests/{id}/reject", friendship.getId())
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friends").isEmpty())
                .andExpect(jsonPath("$.incomingRequests").isEmpty())
                .andExpect(jsonPath("$.outgoingRequests").isEmpty());
    }

    @Test
    void onlyRecipientCanRespondToFriendRequest() throws Exception {
        User requester = userRepository.saveAndFlush(new User(
                "requester-google",
                "Requester",
                "requester@example.com",
                null
        ));
        User recipient = userRepository.saveAndFlush(new User(
                "recipient-google",
                "Recipient",
                "recipient@example.com",
                null
        ));
        Friendship friendship = friendshipRepository.saveAndFlush(new Friendship(requester, recipient, "Want to play?"));

        mockMvc.perform(post("/api/friends/requests/{id}/accept", friendship.getId())
                        .with(oauthUser("requester-google", "Requester", "requester@example.com")))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor oauthUser(String googleSubject, String name, String email) {
        return oauth2Login().attributes(attributes -> {
            attributes.put("sub", googleSubject);
            attributes.put("name", name);
            attributes.put("email", email);
        });
    }
}
