package com.pasoor.invite;

import com.pasoor.friend.FriendshipRepository;
import com.pasoor.match.MatchRepository;
import com.pasoor.match.MatchResponse;
import com.pasoor.match.MatchService;
import com.pasoor.match.MatchStatus;
import com.pasoor.match.PasoorMatch;
import com.pasoor.user.User;
import com.pasoor.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class GameInviteService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameInviteService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final GameInviteRepository inviteRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final MatchService matchService;
    private final String frontendUrl;

    public GameInviteService(
            GameInviteRepository inviteRepository,
            MatchRepository matchRepository,
            UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            MatchService matchService,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.inviteRepository = inviteRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.matchService = matchService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public GameInviteResponse createInvite(User sender, UUID matchId, GameInviteCreateRequest request) {
        String email = normalizeEmail(request.email());
        User recipient = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient user not found."));
        if (sender.getId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot invite yourself.");
        }
        if (!friendshipRepository.existsAcceptedBetweenUsers(sender.getId(), recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only invite accepted friends.");
        }

        PasoorMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found."));
        if (!match.getPlayerOne().getId().equals(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the match creator can invite a friend.");
        }
        if (match.getStatus() != MatchStatus.ACTIVE && match.getStatus() != MatchStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match cannot be invited from its current status.");
        }
        if (match.getPlayerTwo() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match already has a second player.");
        }

        match.setStatus(MatchStatus.WAITING);
        GameInvite invite = inviteRepository.save(new GameInvite(match, sender, recipient, token()));
        String inviteLink = inviteLink(invite.getToken());
        LOGGER.info("Pasoor invite for {}: {}", recipient.getEmail(), inviteLink);

        return GameInviteResponse.from(invite, inviteLink, matchService.responseFor(match));
    }

    @Transactional(readOnly = true)
    public GameInviteResponse getInvite(User user, String token) {
        GameInvite invite = inviteByToken(token);
        if (!invite.getRecipient().getId().equals(user.getId()) && !invite.getSender().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view this invite.");
        }

        return GameInviteResponse.from(invite, inviteLink(invite.getToken()), matchService.responseFor(invite.getMatch()));
    }

    @Transactional
    public GameInviteResponse acceptInvite(User recipient, String token) {
        GameInvite invite = inviteByToken(token);
        if (!invite.getRecipient().getId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the invited friend can accept this invite.");
        }
        if (invite.getStatus() != GameInviteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invite is not pending.");
        }

        PasoorMatch match = invite.getMatch();
        if (match.getStatus() != MatchStatus.WAITING || match.getPlayerTwo() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match is no longer waiting for this invite.");
        }

        invite.setStatus(GameInviteStatus.ACCEPTED);
        invite.setAcceptedAt(Instant.now());
        match.setPlayerTwo(recipient);
        match.setStatus(MatchStatus.ACTIVE);

        return GameInviteResponse.from(invite, inviteLink(invite.getToken()), matchService.responseFor(match));
    }

    private GameInvite inviteByToken(String token) {
        return inviteRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found."));
    }

    private String inviteLink(String token) {
        return frontendUrl + "/invite/" + token;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String token() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
