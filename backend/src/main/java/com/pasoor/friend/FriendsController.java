package com.pasoor.friend;

import com.pasoor.user.User;
import com.pasoor.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
public class FriendsController {
    private final FriendshipService friendshipService;
    private final UserService userService;

    public FriendsController(FriendshipService friendshipService, UserService userService) {
        this.friendshipService = friendshipService;
        this.userService = userService;
    }

    @GetMapping
    public FriendsResponse friends(@AuthenticationPrincipal OAuth2User principal) {
        return friendshipService.friendsFor(currentUser(principal));
    }

    @PostMapping("/requests")
    public FriendsResponse sendRequest(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody FriendRequestCreateRequest request
    ) {
        return friendshipService.sendRequest(currentUser(principal), request);
    }

    @PostMapping("/requests/{id}/accept")
    public FriendsResponse acceptRequest(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id
    ) {
        return friendshipService.acceptRequest(currentUser(principal), id);
    }

    @PostMapping("/requests/{id}/reject")
    public FriendsResponse rejectRequest(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID id
    ) {
        return friendshipService.rejectRequest(currentUser(principal), id);
    }

    private User currentUser(OAuth2User principal) {
        return userService.syncOAuthUser(principal);
    }
}
