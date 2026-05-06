package com.pasoor.match;

import com.pasoor.game.CaptureCardsRequest;
import com.pasoor.game.PlayCardRequest;
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
@RequestMapping("/api/matches")
public class MatchesController {
    private final MatchService matchService;
    private final UserService userService;

    public MatchesController(MatchService matchService, UserService userService) {
        this.matchService = matchService;
        this.userService = userService;
    }

    @PostMapping
    public MatchResponse createMatch(@AuthenticationPrincipal OAuth2User principal) {
        return matchService.createMatch(currentUser(principal));
    }

    @GetMapping("/{matchId}")
    public MatchResponse getMatch(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID matchId) {
        return matchService.getMatch(currentUser(principal), matchId);
    }

    @PostMapping("/{matchId}/exit")
    public MatchResponse exitMatch(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID matchId) {
        return matchService.exitMatch(currentUser(principal), matchId);
    }

    @PostMapping("/{matchId}/end-choice")
    public MatchResponse chooseMatchEnd(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID matchId,
            @RequestBody MatchEndChoiceRequest request
    ) {
        return matchService.chooseMatchEnd(currentUser(principal), matchId, request);
    }

    @PostMapping("/{matchId}/rounds/{roundId}/deal")
    public MatchResponse deal(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID matchId,
            @PathVariable UUID roundId
    ) {
        return matchService.deal(currentUser(principal), matchId, roundId);
    }

    @PostMapping("/{matchId}/rounds/{roundId}/acknowledge")
    public MatchResponse acknowledgeRound(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID matchId,
            @PathVariable UUID roundId
    ) {
        return matchService.acknowledgeRound(currentUser(principal), matchId, roundId);
    }

    @PostMapping("/{matchId}/rounds/{roundId}/play")
    public MatchResponse play(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID matchId,
            @PathVariable UUID roundId,
            @RequestBody PlayCardRequest request
    ) {
        return matchService.play(currentUser(principal), matchId, roundId, request);
    }

    @PostMapping("/{matchId}/rounds/{roundId}/capture")
    public MatchResponse capture(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable UUID matchId,
            @PathVariable UUID roundId,
            @RequestBody CaptureCardsRequest request
    ) {
        return matchService.capture(currentUser(principal), matchId, roundId, request);
    }

    private User currentUser(OAuth2User principal) {
        return userService.syncOAuthUser(principal);
    }
}
