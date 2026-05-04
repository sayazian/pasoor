package com.pasoor.game;

import java.util.List;

public record CaptureCardsRequest(Player player, List<String> capturedTableCardIds) {
}
