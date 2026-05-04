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

Phase 2 uses Spring Security OAuth2 Client for Google sign-in. Configure these environment variables before running the backend with real Google login:

```text
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=your-google-client-id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=your-google-client-secret
FRONTEND_URL=http://localhost:5173
```

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

Phase 6 lets a user invite an accepted friend to a match. The backend logs the invite link instead of sending real email for now.

```text
POST /api/matches/{matchId}/invite
GET /api/invites/{token}
POST /api/invites/{token}/accept
```

Invites are only allowed for accepted friends. Creating an invite moves the match to `WAITING`; accepting the invite sets `playerTwo` and returns the match to `ACTIVE`.

### Commands

```bash
cd backend
mvn test
mvn spring-boot:run
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
