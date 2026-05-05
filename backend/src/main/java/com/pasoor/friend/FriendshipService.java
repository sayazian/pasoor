package com.pasoor.friend;

import com.pasoor.user.User;
import com.pasoor.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public FriendsResponse friendsFor(User currentUser) {
        Instant now = Instant.now();
        List<Friendship> friendships = friendshipRepository.findByRequesterIdOrRecipientId(
                currentUser.getId(),
                currentUser.getId()
        );

        List<FriendSummary> friends = friendships.stream()
                .filter(friendship -> friendship.getStatus() == FriendshipStatus.ACCEPTED)
                .map(friendship -> FriendSummary.from(otherUser(friendship, currentUser), now))
                .sorted(Comparator.comparing(FriendSummary::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<FriendRequestSummary> incomingRequests = friendships.stream()
                .filter(friendship -> friendship.getStatus() == FriendshipStatus.PENDING)
                .filter(friendship -> friendship.getRecipient().getId().equals(currentUser.getId()))
                .map(FriendRequestSummary::from)
                .toList();

        List<FriendRequestSummary> outgoingRequests = friendships.stream()
                .filter(friendship -> friendship.getStatus() == FriendshipStatus.PENDING)
                .filter(friendship -> friendship.getRequester().getId().equals(currentUser.getId()))
                .map(FriendRequestSummary::from)
                .toList();

        return new FriendsResponse(friends, incomingRequests, outgoingRequests);
    }

    @Transactional
    public FriendsResponse sendRequest(User requester, FriendRequestCreateRequest request) {
        String email = normalizeEmail(request.email());
        String message = normalizeMessage(request.message());
        User recipient = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Recipient user was not found."));

        if (recipient.getId().equals(requester.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot send a friend request to yourself.");
        }

        boolean duplicate = friendshipRepository.findBetweenUsers(requester.getId(), recipient.getId()).stream()
                .anyMatch(friendship ->
                        friendship.getStatus() == FriendshipStatus.PENDING ||
                                friendship.getStatus() == FriendshipStatus.ACCEPTED
                );

        if (duplicate) {
            throw new ResponseStatusException(CONFLICT, "A pending or accepted friendship already exists.");
        }

        friendshipRepository.save(new Friendship(requester, recipient, message));
        return friendsFor(requester);
    }

    @Transactional
    public FriendsResponse acceptRequest(User recipient, UUID requestId) {
        Friendship friendship = incomingRequestFor(recipient, requestId);
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
        return friendsFor(recipient);
    }

    @Transactional
    public FriendsResponse rejectRequest(User recipient, UUID requestId) {
        Friendship friendship = incomingRequestFor(recipient, requestId);
        friendship.setStatus(FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);
        return friendsFor(recipient);
    }

    private Friendship incomingRequestFor(User recipient, UUID requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Friend request was not found."));

        if (!friendship.getRecipient().getId().equals(recipient.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Only the recipient can respond to this request.");
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ResponseStatusException(CONFLICT, "Friend request is no longer pending.");
        }

        return friendship;
    }

    private User otherUser(Friendship friendship, User currentUser) {
        return friendship.getRequester().getId().equals(currentUser.getId())
                ? friendship.getRecipient()
                : friendship.getRequester();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Email is required.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String trimmed = message.trim();
        if (trimmed.length() > 500) {
            throw new ResponseStatusException(BAD_REQUEST, "Message must be 500 characters or fewer.");
        }
        return trimmed;
    }
}
