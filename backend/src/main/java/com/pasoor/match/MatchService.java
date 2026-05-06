package com.pasoor.match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pasoor.game.CaptureCardsRequest;
import com.pasoor.game.GamePhase;
import com.pasoor.game.GameService;
import com.pasoor.game.GameState;
import com.pasoor.game.PlayCardRequest;
import com.pasoor.game.Player;
import com.pasoor.game.Score;
import com.pasoor.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MatchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchService.class);
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
        GameRound round = roundRepository.save(new GameRound(match, 1, writeGameState(createDealtGame(Player.ME))));
        LOGGER.info("Match {} created by playerOne {}.", match.getId(), playerOne.getId());

        return response(match, round, playerOne);
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatch(User user, UUID matchId) {
        PasoorMatch match = ownedMatch(user, matchId);
        GameRound round = currentRound(match);

        return response(match, round, user);
    }

    @Transactional
    public MatchResponse exitMatch(User user, UUID matchId) {
        PasoorMatch match = ownedMatch(user, matchId);
        match.setStatus(MatchStatus.ABANDONED);
        match.setExitedBy(user);
        LOGGER.info("Match {} abandoned by user {}.", match.getId(), user.getId());
        return response(match, currentRound(match), user);
    }

    @Transactional
    public MatchResponse acknowledgeRound(User user, UUID matchId, UUID roundId) {
        PasoorMatch match = ownedMatch(user, matchId);
        GameRound round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found."));
        if (!round.getMatch().getId().equals(match.getId()) || round.getStatus() != RoundStatus.FINISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Finished round not found.");
        }

        if (viewerSide(match, user) == MatchPlayerSide.PLAYER_ONE) {
            round.setPlayerOneAcknowledgedAt(Instant.now());
        } else {
            round.setPlayerTwoAcknowledgedAt(Instant.now());
        }

        return response(match, currentRound(match), user);
    }

    @Transactional
    public MatchResponse chooseMatchEnd(User user, UUID matchId, MatchEndChoiceRequest request) {
        PasoorMatch match = ownedMatch(user, matchId);
        if (match.getStatus() != MatchStatus.FINISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match is not finished.");
        }
        if (request.choice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choice is required.");
        }

        if (viewerSide(match, user) == MatchPlayerSide.PLAYER_ONE) {
            match.setPlayerOneEndChoice(request.choice());
        } else {
            match.setPlayerTwoEndChoice(request.choice());
        }

        if (match.getPlayerOneEndChoice() == MatchEndChoice.PLAY_AGAIN
                && match.getPlayerTwoEndChoice() == MatchEndChoice.PLAY_AGAIN
                && match.getRematch() == null
                && match.getPlayerTwo() != null) {
            PasoorMatch rematch = matchRepository.save(new PasoorMatch(match.getPlayerOne()));
            rematch.setPlayerTwo(match.getPlayerTwo());
            GameRound round = roundRepository.save(new GameRound(rematch, 1, writeGameState(createDealtGame(Player.ME))));
            match.setRematch(rematch);
            LOGGER.info("Rematch {} created from finished match {}.", rematch.getId(), match.getId());
            return response(rematch, round, user);
        }

        return response(match, currentRound(match), user);
    }

    @Transactional
    public MatchResponse deal(User user, UUID matchId, UUID roundId) {
        return updateActiveRound(user, matchId, roundId, gameService::deal);
    }

    @Transactional
    public MatchResponse play(User user, UUID matchId, UUID roundId, PlayCardRequest request) {
        PasoorMatch match = ownedMatch(user, matchId);
        PlayCardRequest canonicalRequest = new PlayCardRequest(canonicalRequestPlayer(match, user, request.player()), request.cardId());
        return updateActiveRound(user, matchId, roundId, state -> gameService.playCard(state, canonicalRequest));
    }

    @Transactional
    public MatchResponse capture(User user, UUID matchId, UUID roundId, CaptureCardsRequest request) {
        PasoorMatch match = ownedMatch(user, matchId);
        CaptureCardsRequest canonicalRequest = new CaptureCardsRequest(
                canonicalRequestPlayer(match, user, request.player()),
                request.capturedTableCardIds()
        );
        return updateActiveRound(user, matchId, roundId, state -> gameService.captureCards(state, canonicalRequest));
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
                        writeGameState(createDealtGame(startingPlayerForRound(round.getRoundNumber() + 1)))
                ));
                return response(match, nextRound, user);
            }
        }

        return response(match, round, user);
    }

    private Player canonicalRequestPlayer(PasoorMatch match, User user, Player requestedPlayer) {
        if (requestedPlayer != Player.ME) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot act for the other player.");
        }

        return canonicalPlayer(match, user);
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
                    LOGGER.info("Match {} finished with winner {}.", match.getId(), winner);
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

    private MatchResponse response(PasoorMatch match, GameRound round, User viewer) {
        com.pasoor.game.Player viewerPlayer = canonicalPlayer(match, viewer);
        return MatchResponse.from(
                match,
                viewerSide(match, viewer),
                RoundResponse.from(round, VisibleGameState.from(readGameState(round), viewerPlayer)),
                lastCompletedRound(match, viewerPlayer),
                completedRounds(match, viewerPlayer)
        );
    }

    private RoundResponse lastCompletedRound(PasoorMatch match, Player viewerPlayer) {
        return roundRepository.findFirstByMatchIdAndStatusOrderByFinishedAtDesc(match.getId(), RoundStatus.FINISHED)
                .map(round -> RoundResponse.from(round, VisibleGameState.from(readGameState(round), viewerPlayer)))
                .orElse(null);
    }

    private List<RoundResponse> completedRounds(PasoorMatch match, Player viewerPlayer) {
        return roundRepository.findByMatchIdAndStatusOrderByRoundNumberAsc(match.getId(), RoundStatus.FINISHED)
                .stream()
                .map(round -> RoundResponse.from(round, VisibleGameState.from(readGameState(round), viewerPlayer)))
                .toList();
    }

    public MatchResponse responseFor(PasoorMatch match, User viewer) {
        return response(match, currentRound(match), viewer);
    }

    private Player canonicalPlayer(PasoorMatch match, User user) {
        return match.getPlayerOne().getId().equals(user.getId())
                ? Player.ME
                : Player.OPPONENT;
    }

    private Player startingPlayerForRound(int roundNumber) {
        return roundNumber % 2 == 1 ? Player.ME : Player.OPPONENT;
    }

    private GameState createDealtGame(Player startingPlayer) {
        return gameService.deal(gameService.createGame(startingPlayer));
    }

    private MatchPlayerSide viewerSide(PasoorMatch match, User user) {
        return match.getPlayerOne().getId().equals(user.getId())
                ? MatchPlayerSide.PLAYER_ONE
                : MatchPlayerSide.PLAYER_TWO;
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
