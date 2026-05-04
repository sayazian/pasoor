import '@testing-library/jest-dom/vitest';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import GameBoard from './GameBoard';
import type { Card, GameState, Rank, Suit } from '../types/game';

describe('GameBoard performance-oriented rendering', () => {
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
    expect(screen.getAllByRole('button', { name: /of/i })).toHaveLength(47);
  });
});

function gameStateWithLargeTable(tableSize: number): GameState {
  return {
    deck: [],
    deckCount: 0,
    myHand: [card('MY-ACE', 'SPADES', 'ACE')],
    opponentHand: [card('OPPONENT-KING', 'HEARTS', 'KING')],
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
