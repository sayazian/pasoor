import '@testing-library/jest-dom/vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import GameBoard from './GameBoard';
import type { Card, GameState, Rank, Suit } from '../types/game';
import type { MatchState } from '../types/match';

describe('GameBoard performance-oriented rendering', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders a table with many cards and keeps key controls visible', () => {
    const game = gameStateWithLargeTable(44);

    render(
      <GameBoard
        game={game}
        selectedTableCards={[]}
        error={null}
        onPlayCard={vi.fn()}
        onToggleTableCard={vi.fn()}
        onCapture={vi.fn()}
      />
    );

    expect(screen.getByText('Pasoor')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /capture/i })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /of/i })).toHaveLength(46);
  });

  it('renders face-down cards with only the image-backed card back', () => {
    render(
      <GameBoard
        game={{ ...gameStateWithLargeTable(0), deck: [card('DECK-TWO', 'CLUBS', 'TWO')], deckCount: 1 }}
        selectedTableCards={[]}
        error={null}
        onPlayCard={vi.fn()}
        onToggleTableCard={vi.fn()}
        onCapture={vi.fn()}
      />
    );

    const faceDownCards = screen.getAllByRole('button', { name: /face-down card/i });

    expect(faceDownCards.length).toBeGreaterThan(0);
    expect(faceDownCards.every((cardButton) => cardButton.textContent === '')).toBe(true);
  });

  it('renders opponent hand from count as disabled card backs without leaking card labels', () => {
    const onPlayCard = vi.fn();

    render(
      <GameBoard
        game={{
          ...gameStateWithLargeTable(0),
          opponentHand: [],
          opponentHandCount: 4,
          currentTurn: 'OPPONENT',
          pendingCapturePlayer: null,
          pendingCaptureCard: null
        }}
        selectedTableCards={[]}
        error={null}
        onPlayCard={onPlayCard}
        onToggleTableCard={vi.fn()}
        onCapture={vi.fn()}
      />
    );

    const faceDownCards = screen.getAllByRole('button', { name: /face-down card/i });

    expect(screen.getByText('Waiting for Opponent')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /K of hearts/i })).not.toBeInTheDocument();
    expect(faceDownCards.filter((cardButton) => cardButton.hasAttribute('disabled')).length).toBeGreaterThanOrEqual(4);
    expect(onPlayCard).not.toHaveBeenCalled();
  });

  it('does not show capture controls for opponent pending capture', () => {
    const onToggleTableCard = vi.fn();
    const onCapture = vi.fn();

    render(
      <GameBoard
        game={{
          ...gameStateWithLargeTable(0),
          tableCards: [card('PENDING-FIVE', 'CLUBS', 'FIVE'), card('TABLE-EIGHT', 'HEARTS', 'EIGHT')],
          pendingCapturePlayer: 'OPPONENT',
          pendingCaptureCard: card('PENDING-FIVE', 'CLUBS', 'FIVE')
        }}
        selectedTableCards={[]}
        error={null}
        onPlayCard={vi.fn()}
        onToggleTableCard={onToggleTableCard}
        onCapture={onCapture}
      />
    );

    const tableCard = screen.getByRole('button', { name: /8 of hearts/i });

    expect(screen.queryByRole('button', { name: /^capture$/i })).not.toBeInTheDocument();
    expect(screen.getByText('Waiting for Opponent capture')).toBeInTheDocument();
    expect(tableCard).toHaveAttribute('aria-disabled', 'true');
    tableCard.click();
    expect(onToggleTableCard).not.toHaveBeenCalled();
    expect(onCapture).not.toHaveBeenCalled();
  });

  it('keeps disabled face-up black cards readable', () => {
    render(
      <GameBoard
        game={gameStateWithLargeTable(0)}
        selectedTableCards={[]}
        error={null}
        onPlayCard={vi.fn()}
        onToggleTableCard={vi.fn()}
        onCapture={vi.fn()}
      />
    );

    const disabledClub = screen.getByRole('button', { name: /5 of clubs/i });

    expect(disabledClub).toHaveClass('disabled');
    expect(disabledClub).toHaveAttribute('aria-disabled', 'true');
    expect(disabledClub).not.toHaveAttribute('disabled');
    expect(disabledClub).not.toHaveClass('card-back');
  });

  it('keeps disabled face-up hearts and diamonds red', () => {
    render(
      <GameBoard
        game={{
          ...gameStateWithLargeTable(0),
          tableCards: [card('PENDING-THREE', 'DIAMONDS', 'THREE'), card('TABLE-EIGHT', 'HEARTS', 'EIGHT')],
          pendingCaptureCard: card('PENDING-THREE', 'DIAMONDS', 'THREE')
        }}
        selectedTableCards={[]}
        error={null}
        onPlayCard={vi.fn()}
        onToggleTableCard={vi.fn()}
        onCapture={vi.fn()}
      />
    );

    expect(screen.getByRole('button', { name: /3 of diamonds/i })).toHaveClass('red-card');
    expect(screen.getByRole('button', { name: /8 of hearts/i })).toHaveClass('red-card');
  });

  it('uses player names from the match instead of generic opponent labels', () => {
    render(
      <GameBoard
        game={gameStateWithLargeTable(0)}
        match={matchState()}
        selectedTableCards={[]}
        error={null}
        onPlayCard={vi.fn()}
        onToggleTableCard={vi.fn()}
        onCapture={vi.fn()}
      />
    );

    expect(screen.getByRole('heading', { name: /sahar turn/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Friend' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Opponent' })).not.toBeInTheDocument();
    expect(screen.getByText('Friend taken')).toBeInTheDocument();
    expect(screen.getByText('Sahar taken')).toBeInTheDocument();
  });
});

function gameStateWithLargeTable(tableSize: number): GameState {
  return {
    deck: [],
    deckCount: 0,
    myHand: [card('MY-ACE', 'SPADES', 'ACE')],
    opponentHand: [card('OPPONENT-KING', 'HEARTS', 'KING')],
    opponentHandCount: 1,
    tableCards: [card('PENDING-FIVE', 'CLUBS', 'FIVE'), ...manyCards(tableSize)],
    myCollectedPile: [],
    opponentCollectedPile: [],
    currentTurn: 'ME',
    phase: 'PLAYING',
    initialDealDone: true,
    mySurCount: 0,
    opponentSurCount: 0,
    pendingCapturePlayer: 'ME',
    pendingCaptureCard: card('PENDING-FIVE', 'CLUBS', 'FIVE'),
    lastCapturePlayer: null,
    score: null
  };
}

function manyCards(count: number): Card[] {
  const suits: Suit[] = ['CLUBS', 'DIAMONDS', 'HEARTS', 'SPADES'];
  const ranks: Rank[] = ['ACE', 'TWO', 'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE', 'TEN', 'JACK', 'QUEEN', 'KING'];

  return Array.from({ length: count }, (_, index) => card(`TABLE-${index}`, suits[index % suits.length], ranks[index % ranks.length]));
}

function card(id: string, suit: Suit, rank: Rank): Card {
  const values: Record<Rank, number> = {
    ACE: 1,
    TWO: 2,
    THREE: 3,
    FOUR: 4,
    FIVE: 5,
    SIX: 6,
    SEVEN: 7,
    EIGHT: 8,
    NINE: 9,
    TEN: 10,
    JACK: 11,
    QUEEN: 12,
    KING: 13
  };

  return { id, suit, rank, value: values[rank] };
}

function matchState(): MatchState {
  return {
    id: 'match-1',
    status: 'ACTIVE',
    playerOne: {
      id: 'player-1',
      name: 'Sahar',
      email: 'sahar@example.com',
      avatarUrl: null
    },
    playerTwo: {
      id: 'player-2',
      name: 'Friend',
      email: 'friend@example.com',
      avatarUrl: null
    },
    viewerSide: 'PLAYER_ONE',
    playerOneTotalScore: 0,
    playerTwoTotalScore: 0,
    winnerSide: null,
    winner: null,
    exitedBy: null,
    playerOneEndChoice: null,
    playerTwoEndChoice: null,
    rematchId: null,
    currentRound: {
      id: 'round-1',
      roundNumber: 1,
      status: 'ACTIVE',
      gameState: gameStateWithLargeTable(0),
      playerOneRoundScore: null,
      playerTwoRoundScore: null,
      playerOneAcknowledged: false,
      playerTwoAcknowledged: false
    },
    lastCompletedRound: null,
    completedRounds: []
  };
}
