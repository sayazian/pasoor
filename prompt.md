
Plan the implementation of the following.
# Pasoor Game
Pasoor is a card game with a standard 52 card deck. There are two users.

- Make an app with TypeScript and react on frontend and java backend.
- I want my server to be spring boot.
# UI Description
Create a layout like this:

- There are three columns:
- Left column show the card deck face down
- Middle column has three rows:
  - Top: face down are the opponents cards
  - Middle (called table): This row should be able to show all the cards that are dropped and not collected, face up.
  - Bottom: face up are my cards
- Right column also has two rows:
  - Top my opponent used cards all collected in one place face down
  - Bottom my used cards all collected in one place face down
At the bottom of the page put a button that says "New Game".

# Sequence
When you load the page go through this sequence:

1. Shuffle the full 52 card deck and place it on the left hand column.
2. All the other card places are empty at the beginning.
3. When I click on the deck, deal 4 cards for each user and put 4 cards face up on the table.
4. Then users drop their cards alternatively: pick a card from their hand and put on the table in turns.
5. Users should be able to move cards from the table to used cards pile.
6. As soon as all four cards of both users are dropped, the next round of cards should be dealt:
deal 4 cards to each user.
7. Then steps 4 to 6 should be repeated until all 52 cards are dealt and dropped.
8. Then if the "New Game" is pressed start from step 1.



