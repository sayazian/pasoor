package com.pasoor.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRoundRepository extends JpaRepository<GameRound, UUID> {
    Optional<GameRound> findFirstByMatchIdAndStatusOrderByRoundNumberDesc(UUID matchId, RoundStatus status);

    Optional<GameRound> findFirstByMatchIdOrderByRoundNumberDesc(UUID matchId);

    Optional<GameRound> findFirstByMatchIdAndStatusOrderByFinishedAtDesc(UUID matchId, RoundStatus status);

    List<GameRound> findByMatchIdAndStatusOrderByRoundNumberAsc(UUID matchId, RoundStatus status);
}
