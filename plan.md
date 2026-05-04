# Pasoor Game Implementation Plan

## Goal

Build a two-player Pasoor game app with:

- TypeScript + React frontend
- Java Spring Boot backend
- Standard 52-card deck
- Manual control for both my cards and the opponent's cards
- A deck count showing how many cards remain

## Project Structure

```text
backend/
  build.gradle
  src/main/java/com/pasoor/PasoorApplication.java
  src/main/java/com/pasoor/game/Card.java
  src/main/java/com/pasoor/game/GameState.java
  src/main/java/com/pasoor/game/Player.java
  src/main/java/com/pasoor/game/GamePhase.java
  src/main/java/com/pasoor/game/GameService.java
  src/main/java/com/pasoor/game/GameController.java

frontend/
  package.json
  src/main.tsx
  src/App.tsx
  src/api/gameApi.ts
  src/types/game.ts
  src/components/GameBoard.tsx
  src/components/Card.tsx
  src/components/DeckPile.tsx
  src/components/HandRow.tsx
  src/components/TableRow.tsx
  src/components/CollectedPile.tsx
  src/styles.css
```

## Backend Plan

Create a Spring Boot app that owns the game state and validates moves.

### Core Models

- `Card`
  - `id`
  - `suit`
  - `rank`
  - `value`

- `Player`
  - `ME`
  - `OPPONENT`

- `GamePhase`
  - `NEW`
  - `PLAYING`
  - `FINISHED`

- `GameState`
  - `deck`
  - `myHand`
  - `opponentHand`
  - `tableCards`
  - `myCollectedPile`
  - `opponentCollectedPile`
  - `currentTurn`
  - `phase`
  - `initialDealDone`

The deck count should be derived from `deck.size()` and returned to the frontend as part of the game state.

### API Endpoints

```text
POST /api/game/new
```

Creates a new game, shuffles all 52 cards, places them in the deck, and clears all other areas.

```text
GET /api/game
```

Returns the current game state.

```text
POST /api/game/deal
```

Deals cards from the deck.

Initial deal:

- 4 cards to my hand
- 4 cards to opponent hand
- 4 cards face up to the table

Later deals:

- 4 cards to my hand
- 4 cards to opponent hand

```text
POST /api/game/play
```

Request body:

```json
{
  "player": "ME",
  "cardId": "HEARTS-7"
}
```

Moves a card from the selected player's hand to the table and starts a pending capture decision.

```text
POST /api/game/capture
```

Request body:

```json
{
  "player": "ME",
  "capturedTableCardIds": ["SPADES-4"]
}
```

Validates the selected capture and moves the dropped card plus captured table cards into that player's collected pile.

### Game Flow Rules

1. `newGame()` creates and shuffles a full 52-card deck.
2. Initial state:
   - deck has 52 cards
   - my hand is empty
   - opponent hand is empty
   - table is empty
   - both collected piles are empty
3. The deck displays face down with a count showing `52`.
4. Clicking the deck for the first time calls `deal()`.
5. First deal removes 12 cards from the deck:
   - 4 to my hand
   - 4 to opponent hand
   - 4 to the table
6. The deck count updates to `40`.
7. Players alternate turns.
8. I manually move both players' cards:
   - on my turn, click one of my face-up cards
   - on opponent turn, click one of the opponent's cards
9. A played card moves from that hand to the table.
10. Table cards can be selected and moved to either player's used pile.
11. When both hands are empty:
   - if the deck still has cards, clicking the deck deals 4 cards to each player
   - if the deck is empty, the game ends after all cards have been played
12. When the game ends, the last player who captured receives any cards left on the table.
13. Final scores are calculated and shown.
14. The `New Game` button starts over from step 1.

## Frontend Plan

Create a React TypeScript frontend that renders the board and calls the backend API for all state changes.

### Main Components

- `App`
  - loads or creates the game
  - stores current `GameState`

- `GameBoard`
  - owns the three-column layout

- `DeckPile`
  - shows the deck face down
  - shows the remaining card count on top of the deck
  - calls `/api/game/deal` when clicked

- `HandRow`
  - renders either my hand or opponent hand
  - my cards are face up
  - opponent cards are rendered face up and clickable because I will move them manually

- `TableRow`
  - renders all dropped, uncollected cards face up
  - supports selecting cards for collection

- `CollectedPile`
  - renders a face-down stack
  - shows collected card count

- `Card`
  - renders a single card face up or face down

- `NewGameButton`
  - calls `/api/game/new`

- `ScoreBoard`
  - shows final score after the game finishes
  - shows club counts, card points, and sur points

### Layout

```text
+----------------+------------------------------+----------------------+
| Deck           | Opponent hand                | Opponent used pile   |
| face down      |                              | face down            |
| count on top   | Table cards                  |                      |
|                |                              | My used pile         |
|                | My hand                      | face down            |
+----------------+------------------------------+----------------------+

                       [ New Game ]
```

### UI Behavior

On page load:

1. Call `POST /api/game/new`.
2. Render a shuffled deck face down.
3. Show `52` on top of the deck.
4. Leave all hands, table, and used piles empty.

When the deck is clicked:

1. Call `POST /api/game/deal`.
2. Update the board from the response.
3. Update the deck count.

When a hand card is clicked:

1. Determine whether the clicked card belongs to `ME` or `OPPONENT`.
2. Call `POST /api/game/play`.
3. Move the card to the table and mark it as pending capture.

When table cards are selected:

1. Highlight selected cards.
2. If a dropped card is pending, use `Capture` to call `POST /api/game/capture`.
3. If no capture is available after a card is dropped, the backend automatically advances the turn.
4. Do not show eligible capture-group hints.

When `New Game` is pressed:

1. Call `POST /api/game/new`.
2. Reset the full UI.
3. Show a deck count of `52`.

## Styling Plan

- Use CSS grid for the three-column layout.
- Left column should be narrow and centered around the deck.
- Middle column should be the main play area with three stacked rows.
- Right column should show two collected piles.
- Cards should have stable dimensions so layout does not jump.
- Face-down cards should share one consistent card-back style.
- Face-up cards should clearly show rank and suit.
- Deck count should be visually placed on top of the deck, like a badge or overlay.
- The `New Game` button should sit at the bottom of the page.

## Implementation Order

1. Scaffold the Spring Boot backend.
2. Add card, player, phase, and game state models.
3. Implement deck creation and shuffle.
4. Implement new game, deal, validated play, and capture logic.
5. Add REST controller endpoints.
6. Add backend tests for:
   - new game starts with 52 cards
   - first deal leaves 40 cards in deck
   - later deal removes 8 cards
   - playing a card with no capture moves it to the table
   - valid captures move the played card and captured cards to the selected player's pile
   - a card that can capture cannot be dropped
7. Scaffold the React TypeScript frontend.
8. Add frontend API client.
9. Build the three-column board layout.
10. Add deck count overlay.
11. Wire deck click, hand-card click, table selection, capture, and new game.
12. Run backend and frontend locally.
13. Manually verify the full game sequence.

## Out of Scope For This Version

- Multiplayer networking
- Authentication
- Persistence
- AI opponent
