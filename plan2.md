# Pasoor Improvement Plan

## Goal

Add authenticated users, profile editing, friends, game invitations, selectable visual themes, and match-based Pasoor scoring where players continue rounds until a winner is determined at 72 points.

This plan is split into releasable phases. Each phase should leave the app in a working state and should be deployable independently.

## Current Baseline

The app currently has:

- React + TypeScript frontend
- Spring Boot backend
- Single local Pasoor game
- Manual movement for both players
- Validated capture flow
- End-of-round scoring
- Four saved design references:
  - Classic Green Felt
  - Modern Light Table
  - Persian Tile
  - Dark Card Room

The current game state is in memory. The next features require persistence.

## Target Architecture

### Backend

- Spring Boot
- Spring Security
- Spring Security OAuth2 Client
- Google OAuth2 login
- Spring Data JPA
- PostgreSQL for development and production
- H2 for automated tests
- Mail integration for invitations, initially implemented with a log/stub sender

### Frontend

- React + TypeScript
- React Router
- Existing game board reused inside a new `GamePage`
- App-level authenticated layout
- Theme class applied at the app root based on user profile

### Core Backend Entities

```text
User
- id
- googleSubject
- name
- email
- avatarUrl
- preferredTheme
- createdAt
- updatedAt

Friendship
- id
- requester
- recipient
- status: PENDING, ACCEPTED, REJECTED
- message
- createdAt
- updatedAt

GameInvite
- id
- match
- sender
- recipientEmail
- token
- status: PENDING, ACCEPTED, EXPIRED
- createdAt
- acceptedAt

Match
- id
- playerOne
- playerTwo
- status: WAITING, ACTIVE, FINISHED, ABANDONED
- playerOneTotalScore
- playerTwoTotalScore
- winner
- createdAt
- updatedAt

GameRound
- id
- match
- roundNumber
- status: ACTIVE, FINISHED
- gameStateJson
- playerOneRoundScore
- playerTwoRoundScore
- createdAt
- finishedAt
```

## Phase 1: Persistence Foundation

### Release Goal

Introduce database persistence without changing user-visible gameplay.

### Backend Work

- Add Spring Data JPA.
- Add PostgreSQL configuration.
- Add H2 test configuration.
- Add migration strategy.
  - Prefer Flyway if database schema changes will be managed explicitly.
- Add base audit fields where useful:
  - `createdAt`
  - `updatedAt`
- Add `User` entity and repository, even before login is wired.
- Add backend test configuration using H2.

### Frontend Work

- No major UI changes.
- Add environment configuration for backend URL if not already centralized.

### Acceptance Criteria

- Backend starts with PostgreSQL config.
- Tests run against H2.
- Existing game still works.
- No authentication required yet.

### Risks

- Database setup can slow local development if not documented.
- Entity design should avoid coupling current in-memory game state too early.

## Phase 2: Google Login And Authenticated App Shell

### Release Goal

Users can log in with Google, log out, and see a dashboard.

### Backend Work

- Configure Spring Security OAuth2 Client.
- Configure Google OAuth2 login.
- On successful login:
  - Create `User` if no user exists for Google subject.
  - Update name, email, and avatar URL if changed.
- Add authenticated user endpoint:

```text
GET /api/me
```

- Add logout support.
- Add security rules:
  - Login endpoints public.
  - Static frontend public.
  - `/api/me` authenticated.
  - Existing local game endpoints can remain temporarily public or become authenticated depending on frontend readiness.

### Frontend Work

- Add React Router.
- Add routes:

```text
/login
/dashboard
/game
```

- Add `LoginPage`.
- Add `DashboardPage` with:
  - Profile
  - Create Game
  - Friends
  - Logout
- Add authenticated app state:
  - load `/api/me`
  - redirect unauthenticated users to `/login`

### Acceptance Criteria

- User can log in with Google.
- User lands on dashboard after login.
- User can log out.
- Refreshing the page preserves authenticated session.
- Existing game can still be reached.

### Risks

- Local OAuth callback configuration must be documented.
- CORS/session cookie settings must work with local frontend/backend ports.

## Phase 3: Profile Editing And Theme Preference

### Release Goal

Users can edit their profile name and select one of the four saved visual themes.

### Backend Work

- Add profile update endpoint:

```text
PATCH /api/me/profile
Body: {
  "name": "Player Name",
  "preferredTheme": "CLASSIC_GREEN_FELT"
}
```

- Keep email read-only because it comes from Google.
- Validate theme enum:

```text
CLASSIC_GREEN_FELT
MODERN_LIGHT_TABLE
PERSIAN_TILE
DARK_CARD_ROOM
```

### Frontend Work

- Add `ProfilePage`.
- Show:
  - name field
  - email read-only
  - theme selector
- Add four theme choices using the saved design directions:
  - Classic Green Felt
  - Modern Light Table
  - Persian Tile
  - Dark Card Room
- Apply selected theme to app root via CSS class.
- Add first usable theme styles.
  - It is acceptable in this phase for all themes to share structure but differ in color, border, and surface treatment.

### Acceptance Criteria

- User can edit name.
- User can select a theme.
- Theme persists after refresh.
- Dashboard and game board reflect selected theme.

### Risks

- Theme implementation can become too broad. Keep it CSS-variable based.

## Phase 4: Friends And Friend Requests

### Release Goal

Users can see friends and send/respond to friend requests.

### Backend Work

- Add `Friendship` entity.
- Add endpoints:

```text
GET /api/friends
POST /api/friends/requests
POST /api/friends/requests/{id}/accept
POST /api/friends/requests/{id}/reject
```

- `POST /api/friends/requests` body:

```json
{
  "email": "friend@example.com",
  "message": "Want to play Pasoor?"
}
```

- Rules:
  - Cannot send request to yourself.
  - Cannot duplicate pending or accepted friendship.
  - If recipient user exists, create a pending request linked to that user.
  - If recipient user does not exist yet, either reject for now or store by email.
  - Recommended first release: require recipient user to exist.

### Frontend Work

- Add `FriendsPage`.
- Show:
  - accepted friends
  - incoming pending requests
  - outgoing pending requests
- Add send friend request form:
  - email
  - message
- After send, return to friends list.
- Add accept/reject controls for incoming requests.

### Acceptance Criteria

- User can send a friend request by email.
- Recipient can accept or reject.
- Accepted friends show in both users' friends list.
- Duplicate requests are blocked.

### Risks

- Supporting requests to emails that do not have accounts yet introduces invitation complexity. Keep this separate from game invites.

## Phase 5: Match Model And Multi-Round Scoring

### Release Goal

Introduce `Match` and `GameRound` while still allowing a local/manual two-player game.

### Backend Work

- Add `Match` entity.
- Add `GameRound` entity.
- Move current in-memory game state into `GameRound.gameStateJson`.
- Add match endpoints:

```text
POST /api/matches
GET /api/matches/{matchId}
POST /api/matches/{matchId}/exit
```

- Add round action endpoints:

```text
POST /api/matches/{matchId}/rounds/{roundId}/deal
POST /api/matches/{matchId}/rounds/{roundId}/play
POST /api/matches/{matchId}/rounds/{roundId}/capture
```

- Add match scoring rules:
  - Add each round score to match total.
  - If one player has `>= 72` and the other has `< 72`, that player wins.
  - If both players have `> 72`, higher score wins.
  - If both players are exactly `72`, continue.
  - If both players are `>= 72` and tied, continue.
  - Otherwise continue.
- When continuing, create a new `GameRound`.

### Frontend Work

- Change `GamePage` to load a match by ID.
- Show match score:
  - player one total
  - player two total
  - current round number
- Show winner when match finishes.
- Add exit match button.

### Acceptance Criteria

- Creating a game creates a match.
- Playing a round updates match totals.
- A new round starts automatically if there is no winner.
- Match ends when 72-point rules produce a winner.
- Exiting a match marks it abandoned/discarded.

### Risks

- Serializing game state as JSON is pragmatic but should be encapsulated behind a service.
- Later migrations may need normalized game-round state if querying individual cards becomes important.

## Phase 6: Game Invitations

### Release Goal

Users can create a game, invite a friend, and wait until the friend joins.

### Backend Work

- Add `GameInvite` entity.
- Add endpoints:

```text
POST /api/matches/{matchId}/invite
GET /api/invites/{token}
POST /api/invites/{token}/accept
```

- Invite body:

```json
{
  "email": "friend@example.com"
}
```

- Generate secure random token.
- Link invite to match.
- Initially log invitation link to backend logs.
- Later replace log sender with real email sender.
- Only allow invite to:
  - accepted friend, or
  - known user by email, depending on chosen rule
- Recommended first release: only accepted friends.

### Frontend Work

- Add invite flow from Friends page:
  - click invite friend
  - create match
  - create invite
  - navigate to waiting Game page
- Add waiting state on `GamePage`.
- Add invite accept route:

```text
/invite/:token
```

- When invited user accepts:
  - match gets `playerTwo`
  - match status becomes `ACTIVE`
  - game can begin

### Acceptance Criteria

- User can invite an accepted friend to a game.
- Sender sees waiting screen.
- Recipient can open invite link and join.
- Match starts after friend joins.

### Risks

- Real email delivery requires provider setup.
- Invite links must be secure and expire eventually.

## Phase 7: Real Two-Player Visibility And Turn Enforcement

### Release Goal

Make the game behave like real multiplayer: each player only sees their own hand and cannot act for the other player.

### Backend Work

- Add user-specific game DTOs.
- Hide opponent cards:
  - return opponent hand count
  - do not return opponent card identities
- Enforce actions:
  - only current player can play
  - only current player can capture
  - player can only play cards from their own hand
- Map match players:
  - `playerOne`
  - `playerTwo`
- Add authorization checks on every match action.

### Frontend Work

- Opponent hand renders card backs only.
- Remove manual opponent movement.
- Add waiting indicator when it is opponent's turn.
- Poll match state initially.
  - Later, replace with WebSocket/SSE if needed.

### Acceptance Criteria

- Player cannot see opponent card values.
- Player cannot move opponent cards.
- Wrong-turn API calls are rejected.
- UI clearly shows whose turn it is.

### Risks

- Without WebSocket/SSE, polling may feel delayed but is acceptable for first multiplayer release.

## Phase 8: Email Delivery And Polish

### Release Goal

Make invitations production-like and improve the overall user experience.

### Backend Work

- Add real email provider integration.
- Add invite expiration.
- Add resend invite.
- Add structured error responses.
- Add audit logging for match lifecycle.

### Frontend Work

- Improve loading states.
- Improve empty states.
- Add error banners for:
  - expired invite
  - rejected action
  - abandoned match
- Refine all four visual themes.
- Add responsive QA for mobile/tablet.

### Acceptance Criteria

- Invite email is delivered.
- Expired invites cannot be accepted.
- All major flows have clear loading and error states.
- Four themes are visually distinct and usable.

## Testing Plan

### Backend Tests

- User creation from OAuth profile.
- Profile update.
- Theme validation.
- Friend request creation.
- Friend request accept/reject.
- Duplicate friend request prevention.
- Match creation.
- Invite creation and accept.
- Round score aggregation into match score.
- 72-point winner rules.
- Exact 72 tie continues.
- Both above 72 higher score wins.
- Match exit marks abandoned.
- Opponent-card DTO hides card identities.
- Wrong-user match action is rejected.

### Frontend Checks

- Login redirects.
- Dashboard navigation.
- Profile edit and theme persistence.
- Friends list and request form.
- Create game.
- Waiting for invited friend.
- Accept invite.
- Game page hides opponent cards.
- Match score updates after each round.
- Winner display.
- Exit match.

## Release Sequence Summary

1. Persistence foundation
2. Google login and dashboard
3. Profile editing and theme selection
4. Friends and friend requests
5. Match model and multi-round scoring
6. Game invitations
7. Real two-player visibility and turn enforcement
8. Email delivery and polish

## Recommended Next Step

Start with Phase 1 and Phase 2 together on a short-lived branch. Authentication is easiest to verify once `User` persistence exists, and the dashboard can remain minimal until profile and friends are added.

