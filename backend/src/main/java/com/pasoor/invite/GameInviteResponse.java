package com.pasoor.invite;

import com.pasoor.friend.FriendSummary;
import com.pasoor.match.MatchResponse;

import java.util.UUID;

public record GameInviteResponse(
        UUID id,
        GameInviteStatus status,
        FriendSummary sender,
        FriendSummary recipient,
        String recipientEmail,
        String token,
        MatchResponse match
) {
    static GameInviteResponse from(GameInvite invite, MatchResponse match) {
        return new GameInviteResponse(
                invite.getId(),
                invite.getStatus(),
                FriendSummary.from(invite.getSender()),
                FriendSummary.from(invite.getRecipient()),
                invite.getRecipientEmail(),
                invite.getToken(),
                match
        );
    }
}
