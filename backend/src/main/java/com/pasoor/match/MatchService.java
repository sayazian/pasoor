package com.pasoor.match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasoor.game.CaptureCardsRequest;
import com.pasoor.game.GamePhase;
import com.pasoor.game.GameService;
import com.pasoor.game.GameState;
import com.pasoor.game.PlayCardRequest;
import com.pasoor.game.Score;
import com.pasoor.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class MatchService {
    private final MatchRepository matchRepository;
    private final GameRoundRepository roundRepository;
    private final GameService gameService;
    private final ObjectMapper objectMapper;
    private final MatchScoring matchScoring;

    public MatchService(
            MatchRepository matchRepository,
            GameRoundRepository roundRepository,
            GameService gameService,
            ObjectMapper objectMapper
    ) {
        this.matchRepository = matchRepository;
        this.roundRepository = roundRepository;
        this.gameService = gameService;
        this.objectMapper = objectMapper;
        this.matchScoring = new MatchScoring();
    }

    @Transactional
    public MatchResponse createMatch(User playerOne) {
        PasoorMatch match = matchRepository.save(new PasoorMatch(playerOne));
        GameRound round = roundRepository.save(new GameRound(match, 1, writeGameState(gameService.createGame())));

        return response(match, round);
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatch(User user, UUID matchId) {
        PasoorMatch match = ownedMatch(user, matchId);
        GameRound round = currentRound(match);

        return response(match, round);
    }

    @Transactional
    public MatchResponse exitMatch(User user, UUID matchId) {
        PasoorMatch match = ownedMatch(user, matchId);
        match.setStatus(MatchStatus.ABANDONED);
        return response(match, currentRound(match));
    }

    @Transactional
    public MatchResponse deal(User user, UUID matchId, UUID roundId) {
        return updateActiveRound(user, matchId, roundId, gameService::deal);
    }

    @Transactional
    public MatchResponse play(User user, UUID matchId, UUID roundId, PlayCardRequest request) {
        return updateActiveRound(user, matchId, roundId, state -> gameService.playCard(state, request));
    }

    @Transactional
    public MatchResponse capture(User user, UUID matchId, UUID roundId, CaptureCardsRequest request) {
        return updateActiveRound(user, matchId, roundId, state -> gameService.captureCards(state, request));
    }

    private MatchResponse updateActiveRound(User user, UUID matchId, UUID roundId, RoundAction action) {
        PasoorMatch match = ownedActiveMatch(user, matchId);
        GameRound round = activeRound(match);
        if (!round.getId().equals(roundId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found.");
        }

        GameState nextState;
        try {
            nextState = action.apply(readGameState(round));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }

        round.setGameStateJson(writeGameState(nextState));
        if (nextState.getPhase() == GamePhase.FINISHED) {
            finishRound(match, round, nextState);
            if (match.getStatus() == MatchStatus.ACTIVE) {
                GameRound nextRound = roundRepository.save(new GameRound(
                        match,
                        round.getRoundNumber() + 1,
                        writeGameState(gameService.createGame())
                ));
                return response(match, nextRound);
            }
        }

        return response(match, round);
    }

    private void finishRound(PasoorMatch match, GameRound round, GameState state) {
        Score score = state.getScore();
        if (score == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finished round has no score.");
        }

        round.setStatus(RoundStatus.FINISHED);
        round.setFinishedAt(Instant.now());
        round.setPlayerOneRoundScore(score.myScore());
        round.setPlayerTwoRoundScore(score.opponentScore());

        match.setPlayerOneTotalScore(match.getPlayerOneTotalScore() + score.myScore());
        match.setPlayerTwoTotalScore(match.getPlayerTwoTotalScore() + score.opponentScore());
        matchScoring.winner(match.getPlayerOneTotalScore(), match.getPlayerTwoTotalScore())
                .ifPresent(winner -> {
                    match.setStatus(MatchStatus.FINISHED);
                    match.setWinnerSide(winner);
                    match.setWinner(winner == MatchWinner.PLAYER_ONE ? match.getPlayerOne() : match.getPlayerTwo());
                });
    }

    private PasoorMatch ownedActiveMatch(User user, UUID matchId) {
        PasoorMatch match = ownedMatch(user, matchId);
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match is not active.");
        }

        return match;
    }

    private PasoorMatch ownedMatch(User user, UUID matchId) {
        PasoorMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found."));
        if (!match.getPlayerOne().getId().equals(user.getId())
                && (match.getPlayerTwo() == null || !match.getPlayerTwo().getId().equals(user.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a player in this match.");
        }

        return match;
    }

    private GameRound activeRound(PasoorMatch match) {
        return roundRepository.findFirstByMatchIdAndStatusOrderByRoundNumberDesc(match.getId(), RoundStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active round not found."));
    }

    private GameRound currentRound(PasoorMatch match) {
        return roundRepository.findFirstByMatchIdOrderByRoundNumberDesc(match.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found."));
    }

    private MatchResponse response(PasoorMatch match, GameRound round) {
        return MatchResponse.from(match, RoundResponse.from(round, readGameState(round)));
    }

    private GameState readGameState(GameRound round) {
        try {
            return objectMapper.readValue(round.getGameStateJson(), GameState.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored game state could not be read.", exception);
        }
    }

    private String writeGameState(GameState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Game state could not be stored.", exception);
        }
    }

    private interface RoundAction {
        GameState apply(GameState state);
    }
}
