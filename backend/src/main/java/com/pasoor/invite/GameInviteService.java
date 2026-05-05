package com.pasoor.invite;

import com.pasoor.friend.FriendshipRepository;
import com.pasoor.match.MatchRepository;
import com.pasoor.match.MatchService;
import com.pasoor.match.MatchStatus;
import com.pasoor.match.PasoorMatch;
import com.pasoor.user.User;
import com.pasoor.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
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

    public GameInviteService(
            GameInviteRepository inviteRepository,
            MatchRepository matchRepository,
            UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            MatchService matchService
    ) {
        this.inviteRepository = inviteRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.matchService = matchService;
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
        if (!recipient.isOnline(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend must be online to receive a game invite.");
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
        LOGGER.info("Match {} moved to WAITING after invite {} was created.", match.getId(), invite.getId());

        return GameInviteResponse.from(invite, matchService.responseFor(match, sender));
    }

    @Transactional
    public GameInviteListResponse liveInvites(User recipient) {
        List<GameInviteResponse> liveInvites = inviteRepository
                .findByRecipientIdAndStatusOrderByCreatedAtDesc(recipient.getId(), GameInviteStatus.INVITED)
                .stream()
                .filter(invite -> invite.getMatch().getStatus() == MatchStatus.WAITING)
                .map(invite -> GameInviteResponse.from(invite, matchService.responseFor(invite.getMatch(), recipient)))
                .toList();

        return new GameInviteListResponse(liveInvites);
    }

    @Transactional
    public GameInviteResponse getInvite(User user, String token) {
        GameInvite invite = inviteByToken(token);
        if (!invite.getRecipient().getId().equals(user.getId()) && !invite.getSender().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view this invite.");
        }

        return GameInviteResponse.from(invite, matchService.responseFor(invite.getMatch(), user));
    }

    @Transactional
    public GameInviteResponse acceptInvite(User recipient, String token) {
        GameInvite invite = inviteByToken(token);
        if (!invite.getRecipient().getId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the invited friend can accept this invite.");
        }
        if (invite.getStatus() != GameInviteStatus.INVITED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invite is " + invite.getStatus().name().toLowerCase(Locale.ROOT) + ".");
        }

        PasoorMatch match = invite.getMatch();
        if (match.getStatus() != MatchStatus.WAITING || match.getPlayerTwo() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match is no longer waiting for this invite.");
        }

        invite.setStatus(GameInviteStatus.ACCEPTED);
        invite.setAcceptedAt(Instant.now());
        match.setPlayerTwo(recipient);
        match.setStatus(MatchStatus.ACTIVE);
        LOGGER.info("Invite {} accepted; match {} is ACTIVE with playerTwo {}.", invite.getId(), match.getId(), recipient.getId());

        return GameInviteResponse.from(invite, matchService.responseFor(match, recipient));
    }

    @Transactional
    public GameInviteResponse declineInvite(User recipient, String token) {
        GameInvite invite = inviteByToken(token);
        if (!invite.getRecipient().getId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the invited friend can decline this invite.");
        }
        if (invite.getStatus() != GameInviteStatus.INVITED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invite is " + invite.getStatus().name().toLowerCase(Locale.ROOT) + ".");
        }

        PasoorMatch match = invite.getMatch();
        invite.setStatus(GameInviteStatus.DECLINED);
        if (match.getStatus() == MatchStatus.WAITING && match.getPlayerTwo() == null) {
            match.setStatus(MatchStatus.ABANDONED);
        }
        LOGGER.info("Invite {} declined; match {} is ABANDONED.", invite.getId(), match.getId());

        return GameInviteResponse.from(invite, matchService.responseFor(match, recipient));
    }

    private GameInvite inviteByToken(String token) {
        return inviteRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found."));
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
