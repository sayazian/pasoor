package com.pasoor.invite;

import com.pasoor.user.User;
import com.pasoor.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class GameInvitesController {
    private final GameInviteService inviteService;
    private final UserService userService;

    public GameInvitesController(GameInviteService inviteService, UserService userService) {
        this.inviteService = inviteService;
        this.userService = userService;
    }

    @PostMapping("/api/matches/{matchId}/invite")
    public GameInviteResponse createInvite(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID matchId,
            @RequestBody GameInviteCreateRequest request
    ) {
        return inviteService.createInvite(currentUser(principal), matchId, request);
    }

    @GetMapping("/api/invites/{token}")
    public GameInviteResponse getInvite(@AuthenticationPrincipal OAuth2User principal, @PathVariable String token) {
        return inviteService.getInvite(currentUser(principal), token);
    }

    @PostMapping("/api/invites/{token}/accept")
    public GameInviteResponse acceptInvite(@AuthenticationPrincipal OAuth2User principal, @PathVariable String token) {
        return inviteService.acceptInvite(currentUser(principal), token);
    }

    private User currentUser(OAuth2User principal) {
        return userService.syncOAuthUser(principal);
    }
}
