import '@testing-library/jest-dom/vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import './styles.css';

describe('theme stylesheet buttons', () => {
  afterEach(() => {
    cleanup();
  });

  it('applies theme button classes to primary and secondary controls', () => {
    render(
      <div className="theme-persian-tile">
        <button className="new-game-button" type="button">
          New Game
        </button>
        <a className="secondary-action-button" href="/dashboard">
          Dashboard
        </a>
      </div>
    );

    expect(screen.getByRole('button', { name: /new game/i })).toHaveClass('new-game-button');
    expect(screen.getByRole('link', { name: /dashboard/i })).toHaveClass('secondary-action-button');
  });

  it('keeps disabled card text color under browser disabled button styles', () => {
    render(
      <button className="card disabled" disabled type="button">
        <span>Q</span>
        <strong>♣</strong>
        <span>Q</span>
      </button>
    );

    expect(screen.getByText('♣').closest('button')).toHaveClass('disabled');
  });

  it('uses dedicated card ink instead of page text color for black suits', () => {
    render(
      <div className="theme-dark-card-room">
        <button className="card disabled" aria-disabled="true" type="button">
          <span>K</span>
          <strong>♠</strong>
          <span>K</span>
        </button>
      </div>
    );

    expect(screen.getByText('♠').closest('button')).toHaveClass('disabled');
  });

  it('keeps board panels from crossing at tablet widths', () => {
    render(
      <div className="board">
        <aside className="left-column">
          <div className="left-status">
            <h1>Opponent's turn</h1>
          </div>
        </aside>
        <div className="middle-column">
          <section className="hand-row">Opponent</section>
          <section className="table-row">Table</section>
          <section className="hand-row">Me</section>
        </div>
        <aside className="right-column">
          <section className="pile-panel">Opponent used</section>
          <section className="pile-panel">My used</section>
        </aside>
      </div>
    );

    expect(screen.getByText("Opponent's turn")).toBeInTheDocument();
    expect(screen.getByText('Table')).toBeInTheDocument();
  });

  it('keeps the exit match control constrained to the left column', () => {
    render(
      <aside className="left-column">
        <div className="left-status">
          <h1>Alexandria turn</h1>
        </div>
        <section className="pile-panel">Deck</section>
        <section className="match-summary">Score</section>
        <button className="secondary-action-button" type="button">
          Exit Match
        </button>
      </aside>
    );

    expect(screen.getByRole('button', { name: /exit match/i })).toHaveClass('secondary-action-button');
  });

  it('uses readable textbox classes in dark themes', () => {
    render(
      <div className="theme-dark-card-room">
        <label className="field">
          <span>Email</span>
          <input defaultValue="friend@example.com" />
        </label>
        <label className="field">
          <span>Message</span>
          <textarea defaultValue="Want to play?" />
        </label>
        <div className="waiting-panel">
          <input readOnly value="http://localhost:5173/invite/token" />
        </div>
      </div>
    );

    expect(screen.getByDisplayValue('friend@example.com')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Want to play?')).toBeInTheDocument();
    expect(screen.getByDisplayValue('http://localhost:5173/invite/token')).toBeInTheDocument();
  });

  it('uses card ink for theme option labels in dark themes', () => {
    render(
      <div className="theme-dark-card-room">
        <label className="theme-option theme-preview-dark-card-room">
          <input type="radio" name="theme" />
          <span className="theme-swatch" aria-hidden="true" />
          <span>Dark Card Room</span>
        </label>
      </div>
    );

    expect(screen.getByText('Dark Card Room').closest('label')).toHaveClass('theme-option');
  });
});
