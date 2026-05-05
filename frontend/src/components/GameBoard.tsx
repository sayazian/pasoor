import { RotateCcw } from 'lucide-react';
import type { GameState, Player } from '../types/game';
import type { MatchState } from '../types/match';
import CollectedPile from './CollectedPile';
import DeckPile from './DeckPile';
import HandRow from './HandRow';
import ScoreBoard from './ScoreBoard';
import TableRow from './TableRow';

interface GameBoardProps {
  game: GameState;
  match?: MatchState;
  selectedTableCards: string[];
  error: string | null;
  onDeal: () => void;
  onPlayCard: (player: Player, cardId: string) => void;
  onToggleTableCard: (cardId: string) => void;
  onCapture: () => void;
  onNewGame: () => void;
  onExitMatch?: () => void;
}

export default function GameBoard({
  game,
  match,
  selectedTableCards,
  error,
  onDeal,
  onPlayCard,
  onToggleTableCard,
  onCapture,
  onNewGame,
  onExitMatch
}: GameBoardProps) {
  const isPendingCapture = game.pendingCaptureCard !== null;
  const matchFinished = match?.status === 'FINISHED';
  const matchAbandoned = match?.status === 'ABANDONED';
  const opponentHandCount = game.opponentHandCount ?? game.opponentHand.length;
  const score = match ? visibleMatchScore(match) : null;

  return (
    <section className="game">
      <div className="board">
        <aside className="left-column">
          <div className="left-status">
            <div>
              <p className="eyebrow">Pasoor</p>
              <h1>{game.phase === 'FINISHED' ? 'Game finished' : `${labelForPlayer(game.currentTurn)} turn`}</h1>
              {game.phase === 'PLAYING' && game.currentTurn === 'OPPONENT' && (
                <p className="turn-note">Waiting for opponent</p>
              )}
            </div>
          </div>
          <DeckPile count={game.deckCount} phase={game.phase} onDeal={onDeal} />
          {match && (
            <div className="match-summary" aria-label="Match score">
              <div>
                <span>Round</span>
                <strong>{match.currentRound.roundNumber}</strong>
              </div>
              <div>
                <span>Me</span>
                <strong>{score?.myTotalScore ?? 0}</strong>
              </div>
              <div>
                <span>Opponent</span>
                <strong>{score?.opponentTotalScore ?? 0}</strong>
              </div>
              {matchFinished && <p>{winnerLabel(match)} wins the match</p>}
              {matchAbandoned && <p>Match exited</p>}
            </div>
          )}
          <button className="new-game-button" type="button" onClick={onNewGame}>
            <RotateCcw size={18} aria-hidden="true" />
            New Game
          </button>
          <a className="secondary-action-button" href="/dashboard">
            Dashboard
          </a>
          {onExitMatch && match?.status === 'ACTIVE' && (
            <button className="secondary-action-button" type="button" onClick={onExitMatch}>
              Exit Match
            </button>
          )}
        </aside>

        <div className="middle-column">
          <HandRow
            title="Opponent"
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
            onToggleCard={onToggleTableCard}
            onCapture={onCapture}
          />
          <HandRow
            title="Me"
            player="ME"
            cards={game.myHand}
            currentTurn={game.currentTurn}
            disabled={isPendingCapture}
            onPlayCard={onPlayCard}
          />
        </div>

        <aside className="right-column">
          <CollectedPile title="Opponent used" count={game.opponentCollectedPile.length} surCount={game.opponentSurCount} />
          <CollectedPile title="My used" count={game.myCollectedPile.length} surCount={game.mySurCount} />
        </aside>
      </div>

      {error && <p className="error-message">{error}</p>}

      {game.phase === 'FINISHED' && game.score && <ScoreBoard score={game.score} />}

    </section>
  );
}

function labelForPlayer(player: Player) {
  return player === 'ME' ? 'My' : "Opponent's";
}

function winnerLabel(match: MatchState) {
  if (match.winnerSide === match.viewerSide) {
    return 'Me';
  }
  if (match.winnerSide === 'PLAYER_ONE') {
    return match.playerOne.name;
  }
  if (match.winnerSide === 'PLAYER_TWO') {
    return match.playerTwo?.name ?? 'Opponent';
  }

  return 'Winner';
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
