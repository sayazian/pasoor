import '@testing-library/jest-dom/vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import GameBoard from './GameBoard';
import type { Card, GameState, Rank, Suit } from '../types/game';

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
        onDeal={vi.fn()}
        onPlayCard={vi.fn()}
        onToggleTableCard={vi.fn()}
        onCapture={vi.fn()}
        onNewGame={vi.fn()}
      />
    );

    expect(screen.getByText('Pasoor')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /new game/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /capture/i })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /of/i })).toHaveLength(46);
  });

  it('renders face-down cards with only the image-backed card back', () => {
    render(
      <GameBoard
        game={{ ...gameStateWithLargeTable(0), deck: [card('DECK-TWO', 'CLUBS', 'TWO')], deckCount: 1 }}
        selectedTableCards={[]}
        error={null}
        onDeal={vi.fn()}
        onPlayCard={vi.fn()}
        onToggleTableCard={vi.fn()}
        onCapture={vi.fn()}
        onNewGame={vi.fn()}
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
        game={{ ...gameStateWithLargeTable(0), opponentHand: [], opponentHandCount: 4, currentTurn: 'OPPONENT' }}
        selectedTableCards={[]}
        error={null}
        onDeal={vi.fn()}
        onPlayCard={onPlayCard}
        onToggleTableCard={vi.fn()}
        onCapture={vi.fn()}
        onNewGame={vi.fn()}
      />
    );

    const faceDownCards = screen.getAllByRole('button', { name: /face-down card/i });

    expect(screen.getByText('Waiting for opponent')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /K of hearts/i })).not.toBeInTheDocument();
    expect(faceDownCards.filter((cardButton) => cardButton.hasAttribute('disabled')).length).toBeGreaterThanOrEqual(4);
    expect(onPlayCard).not.toHaveBeenCalled();
  });

  it('keeps disabled face-up black cards readable', () => {
    render(
      <GameBoard
        game={gameStateWithLargeTable(0)}
        selectedTableCards={[]}
        error={null}
        onDeal={vi.fn()}
        onPlayCard={vi.fn()}
        onToggleTableCard={vi.fn()}
        onCapture={vi.fn()}
        onNewGame={vi.fn()}
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
        onDeal={vi.fn()}
        onPlayCard={vi.fn()}
        onToggleTableCard={vi.fn()}
        onCapture={vi.fn()}
        onNewGame={vi.fn()}
      />
    );

    expect(screen.getByRole('button', { name: /3 of diamonds/i })).toHaveClass('red-card');
    expect(screen.getByRole('button', { name: /8 of hearts/i })).toHaveClass('red-card');
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
