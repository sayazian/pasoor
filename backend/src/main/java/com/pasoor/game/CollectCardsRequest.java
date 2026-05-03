package com.pasoor.game;

import java.util.List;

public record CollectCardsRequest(Player player, List<String> cardIds) {
}
