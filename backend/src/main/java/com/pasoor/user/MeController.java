package com.pasoor.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {
    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public AuthenticatedUser me(@AuthenticationPrincipal OAuth2User principal) {
        return AuthenticatedUser.from(userService.syncOAuthUser(principal));
    }

    @PatchMapping("/me/profile")
    public AuthenticatedUser updateProfile(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody ProfileUpdateRequest request
    ) {
        User user = userService.syncOAuthUser(principal);
        return AuthenticatedUser.from(userService.updateProfile(user, request));
    }
}
