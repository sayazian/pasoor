# Pasoor

Pasoor is a React + TypeScript frontend with a Java Spring Boot backend.

## Backend

The backend lives in `backend/`.

### Database

The backend uses Spring Data JPA and Flyway.

Default development database configuration:

```text
DATABASE_URL=jdbc:postgresql://localhost:5432/pasoor
DATABASE_USERNAME=pasoor
DATABASE_PASSWORD=pasoor
```

These values can be overridden with environment variables.

Tests use H2 with the `test` Spring profile and run Flyway migrations against the in-memory database.

### Google OAuth

Phase 2 uses Spring Security OAuth2 Client for Google sign-in.

For local development, keep your real Google values in the ignored local profile file:

```bash
cp backend/src/main/resources/application-local.properties.example backend/src/main/resources/application-local.properties
```

Then edit `backend/src/main/resources/application-local.properties` with your real local Google client values. That file is ignored by Git, so changing it for local work will not create a GitHub change or trigger a Railway redeploy.

Run the backend locally with the `local` profile:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Railway production values should stay in Railway environment variables, not in committed files:

```text
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=your-google-client-id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=your-google-client-secret
FRONTEND_URL=http://localhost:5173
```

When the deployed frontend and backend use different Railway hostnames, configure the backend service with production session cookie settings:

```text
SESSION_COOKIE_SAME_SITE=none
SESSION_COOKIE_SECURE=true
```

The deployed frontend service runs `frontend/server.mjs`, which serves the built React app and proxies `/api`, `/oauth2`, `/login`, and `/logout` to the backend. Configure the frontend service with:

```text
VITE_API_BASE_URL=
BACKEND_URL=http://backend.railway.internal:8080
```

With that setup, browser requests use the frontend origin and the session cookie belongs to `pasoor.up.railway.app`.

Register this redirect URI in Google Cloud:

```text
http://localhost:8080/login/oauth2/code/google
```

The authenticated user endpoint is `GET /api/me`, and logout is `POST /api/logout`. Existing `/api/game/**` endpoints remain public so the legacy local game flow can keep working while authenticated match ownership is introduced.

### Matches

Phase 5 stores games inside authenticated matches and rounds. The frontend game page now creates or loads matches through:

```text
POST /api/matches
GET /api/matches/{matchId}
POST /api/matches/{matchId}/exit
POST /api/matches/{matchId}/rounds/{roundId}/deal
POST /api/matches/{matchId}/rounds/{roundId}/play
POST /api/matches/{matchId}/rounds/{roundId}/capture
```

Each completed round updates match totals. If the 72-point winner rules do not produce a winner, the backend automatically creates the next round.

### Game Invites

Game invites are live-only. A user can invite an accepted friend only while that friend is online.

```text
POST /api/matches/{matchId}/invite
GET /api/invites
GET /api/invites/{token}
POST /api/invites/{token}/accept
POST /api/invites/{token}/decline
```

Creating an invite moves the match to `WAITING` and the invited online friend sees a popup from the frontend's `GET /api/invites` polling. Accepting sets `playerTwo` and returns the match to `ACTIVE`; declining marks the invite `DECLINED` and abandons the waiting match so the inviter is informed.

### Two-Player Visibility

Phase 7 makes match game state viewer-specific. Match responses include `viewerSide`, return the authenticated player's cards in `currentRound.gameState.myHand`, and return only `opponentHandCount` for the other player's hand. Opponent card identities are not returned by match APIs.

API errors are returned as structured JSON with `timestamp`, `status`, `error`, `code`, `message`, and `path`.

For match play and capture actions, a user can only act as their own match side, and turn validation remains owned by the backend game service. The frontend renders opponent cards as disabled backs and polls the match while it is active or waiting.

### Commands

```bash
cd backend
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Frontend

The frontend lives in `frontend/`.

### Commands

```bash
cd frontend
npm run test
npm run build
npm run dev
```
