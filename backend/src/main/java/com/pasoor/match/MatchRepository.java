package com.pasoor.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchRepository extends JpaRepository<PasoorMatch, UUID> {
}
