package com.pasoor.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasoor.friend.Friendship;
import com.pasoor.friend.FriendshipRepository;
import com.pasoor.game.GameState;
import com.pasoor.game.Player;
import com.pasoor.invite.GameInviteRepository;
import com.pasoor.match.GameRound;
import com.pasoor.match.GameRoundRepository;
import com.pasoor.match.MatchRepository;
import com.pasoor.match.MatchStatus;
import com.pasoor.match.MatchWinner;
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

import java.time.Instant;
import java.util.UUID;

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
                .andExpect(jsonPath("$.currentRound.gameState.deckCount").value(40))
                .andExpect(jsonPath("$.currentRound.gameState.myHand.length()").value(4))
                .andExpect(jsonPath("$.currentRound.gameState.tableCards.length()").value(4));

        assertThat(matchRepository.count()).isEqualTo(1);
        assertThat(roundRepository.count()).isEqualTo(1);
    }

    @Test
    void matchDealRejectsWhenHandsAreAlreadyDealt() throws Exception {
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
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/matches/{matchId}", matchId)
                        .with(oauthUser("player-google", "Player", "player@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRound.gameState.deckCount").value(40));
    }

    @Test
    void matchRejectsActingForOpponentEvenBeforeSecondPlayerJoins() throws Exception {
        String createResponse = mockMvc.perform(post("/api/matches")
                        .with(oauthUser("player-google", "Player", "player@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode createJson = objectMapper.readTree(createResponse);
        String matchId = createJson.get("id").asText();
        String roundId = createJson.get("currentRound").get("id").asText();

        mockMvc.perform(post("/api/matches/{matchId}/rounds/{roundId}/play", matchId, roundId)
                        .with(oauthUser("player-google", "Player", "player@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"player":"OPPONENT","cardId":"CLUBS-TWO"}
                                """))
                .andExpect(status().isForbidden());
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
                .andExpect(jsonPath("$.status").value("ABANDONED"))
                .andExpect(jsonPath("$.exitedBy.email").value("player@example.com"));
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
                .andExpect(jsonPath("$.status").value("INVITED"))
                .andExpect(jsonPath("$.recipient.email").value("recipient@example.com"))
                .andExpect(jsonPath("$.match.status").value("WAITING"));
    }

    @Test
    void invitedFriendCanListLiveGameInvitesAfterLoggingIn() throws Exception {
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
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/invites")
                .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liveInvites[0].status").value("INVITED"))
                .andExpect(jsonPath("$.liveInvites[0].sender.email").value("sender@example.com"))
                .andExpect(jsonPath("$.liveInvites[0].match.status").value("WAITING"));
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
    void inviteRequiresOnlineFriend() throws Exception {
        User sender = userRepository.saveAndFlush(new User("sender-google", "Sender", "sender@example.com", null));
        User recipient = userRepository.saveAndFlush(new User("recipient-google", "Recipient", "recipient@example.com", null));
        Friendship friendship = friendshipRepository.saveAndFlush(new Friendship(sender, recipient, "Want to play?"));
        mockMvc.perform(post("/api/friends/requests/{id}/accept", friendship.getId())
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk());
        recipient = userRepository.findByEmail("recipient@example.com").orElseThrow();
        recipient.markSeen(Instant.now().minusSeconds(120));
        userRepository.saveAndFlush(recipient);
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Friend must be online to receive a game invite."));
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
    void invitedFriendDeclinesInviteAndAbandonsMatch() throws Exception {
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

        mockMvc.perform(post("/api/invites/{token}/decline", token)
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.match.status").value("ABANDONED"));
    }

    @Test
    void twoPlayerMatchResponsesHideOpponentHandIdentities() throws Exception {
        TwoPlayerMatch twoPlayerMatch = activeTwoPlayerMatch();
        String playerOneResponse = mockMvc.perform(get("/api/matches/{matchId}", twoPlayerMatch.matchId())
                        .with(oauthUser("sender-google", "Sender", "sender@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerSide").value("PLAYER_ONE"))
                .andExpect(jsonPath("$.currentRound.gameState.myHand.length()").value(4))
                .andExpect(jsonPath("$.currentRound.gameState.opponentHand").isEmpty())
                .andExpect(jsonPath("$.currentRound.gameState.opponentHandCount").value(4))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode playerOneCards = objectMapper.readTree(playerOneResponse)
                .get("currentRound")
                .get("gameState")
                .get("myHand");

        String recipientResponse = mockMvc.perform(get("/api/matches/{matchId}", twoPlayerMatch.matchId())
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerSide").value("PLAYER_TWO"))
                .andExpect(jsonPath("$.currentRound.gameState.myHand.length()").value(4))
                .andExpect(jsonPath("$.currentRound.gameState.opponentHand").isEmpty())
                .andExpect(jsonPath("$.currentRound.gameState.opponentHandCount").value(4))
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (JsonNode card : playerOneCards) {
            assertThat(recipientResponse).doesNotContain(card.get("id").asText());
        }
    }

    @Test
    void twoPlayerMatchTranslatesViewerActionsAndRejectsActingForOpponent() throws Exception {
        TwoPlayerMatch twoPlayerMatch = activeTwoPlayerMatch();
        String playerOneResponse = mockMvc.perform(get("/api/matches/{matchId}", twoPlayerMatch.matchId())
                        .with(oauthUser("sender-google", "Sender", "sender@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String playerOneCardId = objectMapper.readTree(playerOneResponse)
                .get("currentRound")
                .get("gameState")
                .get("myHand")
                .get(0)
                .get("id")
                .asText();
        String playerTwoResponse = mockMvc.perform(get("/api/matches/{matchId}", twoPlayerMatch.matchId())
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String playerTwoCardId = objectMapper.readTree(playerTwoResponse)
                .get("currentRound")
                .get("gameState")
                .get("myHand")
                .get(0)
                .get("id")
                .asText();

        mockMvc.perform(post("/api/matches/{matchId}/rounds/{roundId}/play", twoPlayerMatch.matchId(), twoPlayerMatch.roundId())
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"player":"ME","cardId":"%s"}
                                """.formatted(playerOneCardId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/matches/{matchId}/rounds/{roundId}/play", twoPlayerMatch.matchId(), twoPlayerMatch.roundId())
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"player":"OPPONENT","cardId":"%s"}
                                """.formatted(playerOneCardId)))
                .andExpect(status().isForbidden());

        GameRound round = roundRepository.findById(java.util.UUID.fromString(twoPlayerMatch.roundId()))
                .orElseThrow();
        GameState state = objectMapper.readValue(round.getGameStateJson(), GameState.class);
        state.setCurrentTurn(Player.OPPONENT);
        round.setGameStateJson(objectMapper.writeValueAsString(state));
        roundRepository.saveAndFlush(round);

        mockMvc.perform(post("/api/matches/{matchId}/rounds/{roundId}/play", twoPlayerMatch.matchId(), twoPlayerMatch.roundId())
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"player":"ME","cardId":"%s"}
                                """.formatted(playerTwoCardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerSide").value("PLAYER_TWO"));
    }

    @Test
    void acknowledgeRoundStoresViewerAcknowledgement() throws Exception {
        String createResponse = mockMvc.perform(post("/api/matches")
                        .with(oauthUser("player-google", "Player", "player@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode createJson = objectMapper.readTree(createResponse);
        UUID matchId = UUID.fromString(createJson.get("id").asText());
        UUID roundId = UUID.fromString(createJson.get("currentRound").get("id").asText());
        GameRound round = roundRepository.findById(roundId).orElseThrow();
        GameState state = objectMapper.readValue(round.getGameStateJson(), GameState.class);
        state.setScore(new com.pasoor.game.Score(7, 4, 4, 3, 0, 0, 3, 1, 1, 0, 2, 1, 0, 0, 1, 0));
        round.setGameStateJson(objectMapper.writeValueAsString(state));
        round.setStatus(com.pasoor.match.RoundStatus.FINISHED);
        round.setFinishedAt(Instant.now());
        round.setPlayerOneRoundScore(7);
        round.setPlayerTwoRoundScore(4);
        roundRepository.saveAndFlush(round);

        mockMvc.perform(post("/api/matches/{matchId}/rounds/{roundId}/acknowledge", matchId, roundId)
                        .with(oauthUser("player-google", "Player", "player@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastCompletedRound.playerOneAcknowledged").value(true))
                .andExpect(jsonPath("$.lastCompletedRound.playerTwoAcknowledged").value(false));
    }

    @Test
    void bothPlayersChoosingPlayAgainCreatesRematch() throws Exception {
        TwoPlayerMatch twoPlayerMatch = activeTwoPlayerMatch();
        UUID matchId = UUID.fromString(twoPlayerMatch.matchId());
        var match = matchRepository.findById(matchId).orElseThrow();
        match.setStatus(MatchStatus.FINISHED);
        match.setPlayerOneTotalScore(72);
        match.setPlayerTwoTotalScore(58);
        match.setWinnerSide(MatchWinner.PLAYER_ONE);
        match.setWinner(match.getPlayerOne());
        matchRepository.saveAndFlush(match);

        mockMvc.perform(post("/api/matches/{matchId}/end-choice", matchId)
                        .with(oauthUser("sender-google", "Sender", "sender@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"choice":"PLAY_AGAIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(matchId.toString()))
                .andExpect(jsonPath("$.playerOneEndChoice").value("PLAY_AGAIN"));

        mockMvc.perform(post("/api/matches/{matchId}/end-choice", matchId)
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"choice":"PLAY_AGAIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(matchId.toString())))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentRound.gameState.deckCount").value(40));
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

    private TwoPlayerMatch activeTwoPlayerMatch() throws Exception {
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
        JsonNode createdMatch = objectMapper.readTree(createResponse);
        String matchId = createdMatch.get("id").asText();
        String roundId = createdMatch.get("currentRound").get("id").asText();
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
        mockMvc.perform(post("/api/invites/{token}/accept", token)
                        .with(oauthUser("recipient-google", "Recipient", "recipient@example.com")))
                .andExpect(status().isOk());

        return new TwoPlayerMatch(matchId, roundId);
    }

    private record TwoPlayerMatch(String matchId, String roundId) {
    }
}
