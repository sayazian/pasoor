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

