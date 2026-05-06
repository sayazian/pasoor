import type { GameState, Player } from '../types/game';
import type { MatchState } from '../types/match';
import CollectedPile from './CollectedPile';
import DeckPile from './DeckPile';
import HandRow from './HandRow';
import TableRow from './TableRow';

interface GameBoardProps {
  game: GameState;
  match?: MatchState;
  selectedTableCards: string[];
  error: string | null;
  onPlayCard: (player: Player, cardId: string) => void;
  onToggleTableCard: (cardId: string) => void;
  onCapture: () => void;
  onExitMatch?: () => void;
}

export default function GameBoard({
  game,
  match,
  selectedTableCards,
  error,
  onPlayCard,
  onToggleTableCard,
  onCapture,
  onExitMatch
}: GameBoardProps) {
  const isPendingCapture = game.pendingCaptureCard !== null;
  const matchFinished = match?.status === 'FINISHED';
  const matchAbandoned = match?.status === 'ABANDONED';
  const opponentHandCount = game.opponentHandCount ?? game.opponentHand.length;
  const score = match ? visibleMatchScore(match) : null;
  const playerNames = visiblePlayerNames(match);
  const tablePrompt = tableStatusPrompt(game, playerNames.OPPONENT);
  const dealNumber = currentDealNumber(game);

  return (
    <section className="game">
      <div className="board">
        <aside className="left-column">
          <div className="left-status">
            <div>
              <p className="eyebrow">Pasoor</p>
              <h1>{game.phase === 'FINISHED' ? 'Game finished' : `${possessive(playerNames[game.currentTurn])} turn`}</h1>
            </div>
          </div>
          <DeckPile count={game.deckCount} phase={game.phase} />
          {match && (
            <div className="match-summary" aria-label="Match score">
              <div>
                <span>Game</span>
                <strong>{match.currentRound.roundNumber}</strong>
              </div>
              <div>
                <span>Round</span>
                <strong>{dealNumber}</strong>
              </div>
              <div>
                <span>{playerNames.ME}</span>
                <strong>{score?.myTotalScore ?? 0}</strong>
              </div>
              <div>
                <span>{playerNames.OPPONENT}</span>
                <strong>{score?.opponentTotalScore ?? 0}</strong>
              </div>
              {matchFinished && <p>{winnerLabel(match)}</p>}
              {matchAbandoned && <p>Match exited</p>}
            </div>
          )}
          {onExitMatch && match?.status === 'ACTIVE' && (
            <button className="secondary-action-button" type="button" onClick={onExitMatch}>
              Exit Match
            </button>
          )}
        </aside>

        <div className="middle-column">
          <HandRow
            title={playerNames.OPPONENT}
            player="OPPONENT"
            cards={game.opponentHand}
            cardCount={opponentHandCount}
            currentTurn={game.currentTurn}
            faceDown
            disabled
          />
          <TableRow
            cards={game.tableCards}
            selectedCardIds={selectedTableCards}
            pendingCaptureCardId={game.pendingCaptureCard?.id ?? null}
            pendingCapturePlayer={game.pendingCapturePlayer}
            prompt={tablePrompt}
            onToggleCard={onToggleTableCard}
            onCapture={onCapture}
          />
          <HandRow
            title={playerNames.ME}
            player="ME"
            cards={game.myHand}
            currentTurn={game.currentTurn}
            disabled={isPendingCapture}
            onPlayCard={onPlayCard}
          />
        </div>

        <aside className="right-column">
          <CollectedPile title={`${possessive(playerNames.OPPONENT)} taken cards`} count={game.opponentCollectedPile.length} surCount={game.opponentSurCount} />
          <CollectedPile title={`${possessive(playerNames.ME)} taken cards`} count={game.myCollectedPile.length} surCount={game.mySurCount} />
        </aside>
      </div>

      {error && <p className="error-message">{error}</p>}
    </section>
  );
}

function winnerLabel(match: MatchState) {
  if (match.winnerSide === match.viewerSide) {
    return 'You won the match';
  }
  if (match.winnerSide === 'PLAYER_ONE') {
    return `${firstName(match.playerOne.name, 'Opponent')} won the match`;
  }
  if (match.winnerSide === 'PLAYER_TWO') {
    return `${firstName(match.playerTwo?.name, 'Opponent')} won the match`;
  }

  return 'Match finished';
}

function visiblePlayerNames(match?: MatchState): Record<Player, string> {
  if (!match) {
    return { ME: 'You', OPPONENT: 'Opponent' };
  }

  if (match.viewerSide === 'PLAYER_TWO') {
    return {
      ME: firstName(match.playerTwo?.name, 'You'),
      OPPONENT: firstName(match.playerOne.name, 'Opponent')
    };
  }

  return {
    ME: firstName(match.playerOne.name, 'You'),
    OPPONENT: firstName(match.playerTwo?.name, 'Opponent')
  };
}

function visibleMatchScore(match: MatchState) {
  if (match.viewerSide === 'PLAYER_TWO') {
    return {
      myTotalScore: match.playerTwoTotalScore,
      opponentTotalScore: match.playerOneTotalScore
    };
  }

  return {
    myTotalScore: match.playerOneTotalScore,
    opponentTotalScore: match.playerTwoTotalScore
  };
}

function tableStatusPrompt(game: GameState, opponentName: string) {
  if (game.pendingCapturePlayer === 'ME') {
    return 'Waiting for your capture';
  }
  if (game.pendingCapturePlayer === 'OPPONENT') {
    return `Waiting for ${possessive(opponentName)} capture`;
  }
  if (game.currentTurn === 'ME') {
    return 'Play a card';
  }

  return `Waiting for ${opponentName}`;
}

function currentDealNumber(game: GameState) {
  if (game.phase === 'NEW' && !game.initialDealDone) {
    return 0;
  }

  return Math.max(1, Math.min(6, 1 + Math.floor((40 - game.deckCount) / 8)));
}

function possessive(name: string) {
  return `${name}'s`;
}

function firstName(name: string | null | undefined, fallback: string) {
  const trimmed = name?.trim();
  return trimmed ? trimmed.split(/\s+/)[0] : fallback;
}
