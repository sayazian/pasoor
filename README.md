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

The authenticated user endpoint is `GET /api/me`, and logout is `POST /api/logout`. Existing `/api/game/**` endpoints remain public during this phase so the current game flow can keep working while authenticated match ownership is introduced later.

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
