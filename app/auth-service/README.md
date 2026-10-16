# Auth Service

NestJS-based authentication and authorization service for TheUnderFrogs trading platform. Manages user authentication, JWT token generation, and user credentials.

## Architecture

The service uses NestJS **feature modules**. Inside each module, files are grouped by **layer**, the same
way the Java services group theirs:

```
src/
  main.ts, app.module.ts, app.controller.ts   bootstrap, root module, GET /health
  config/          env validation (Joi), database config, RS256 signing key loading
  common/
    decorators/    @Public, @Roles, @CurrentUser
    dtos/          shared response shapes (ErrorResponse, CurrentUser)
    exceptions/    custom exceptions (ValidationException -> 422 VAL-422)
    filters/       HttpExceptionFilter -> { errorCode, message }
    guards/        JwtAuthGuard (RS256), RolesGuard
    validation/    global ValidationPipe, malformed-JSON adapter
  modules/
    <module>/
      <module>.module.ts
      controllers/    HTTP endpoints
      services/       business logic
      repositories/   data access (TypeORM)
      entities/       database models
      dtos/           request/response objects and validation rules
      __tests__/      tests that span several layers (HTTP, end to end)
```

Unit tests sit next to the file they test (`services/auth.service.spec.ts`).

### Modules

| Module | Endpoints | Responsibilities |
|---|---|---|
| **auth** | `POST /auth/register`, `POST /auth/login`, `POST /auth/token`, `GET /auth/me` | Register (creates the user and a trading account in accounts-service), login, client credentials for services, current user. `services/accounts-service-client.ts` calls accounts-service with an RS256 service token. |
| **users** | `POST/GET/PUT/DELETE /users` | User records, argon2id password hashing with BCrypt fallback and rehash on login (`services/password-hasher.service.ts`), password policy (`dtos/validation.ts`) |
| **tokens** | `GET /.well-known/jwks.json` | RS256 access and service tokens (`services/token.service.ts`), refresh tokens stored as SHA-256 hashes (`services/refresh-tokens.service.ts`), public JWKS |
| **clients** | (used by `POST /auth/token`) | Service clients such as trade-executor, with argon2id-hashed secrets |

### Layered design

```
Controller (HTTP) -> Service (business logic) -> Repository (data access) -> Entity (database model)
```

## Database

### Postgres 15 (Alpine)

**Schema** (`db/schema/`):
1. **01-users.sql**: users (username, email, password_hash, full_name, roles, account_id, lockout columns)
2. **02-auth.sql**: login tracking per user
3. **03-tokens.sql**: refresh tokens (SHA-256 hash only, expiry, revocation)
4. **04-service-clients.sql**: service clients (client_id, argon2id secret hash, roles)

**Seed data** (`db/seed/`): the 11 seed users (ids 1-11, linked to ACC0001-ACC0011) with their original
BCrypt hashes, re-hashed to argon2id on first login. The trade-executor client is registered by
`scripts/register-service-client.sh`, which the setup pipeline runs.

### Connections

| Environment | Host       | Port | Database |
|-------------|------------|------|----------|
| Docker      | auth-db    | 5432 | auth_db  |
| Local Dev   | localhost  | 5432 | auth_db  |

## Configuration

### Environment Variables

Required variables (app refuses to start without them):

```env
# Database
DB_HOST=auth-db              # Postgres hostname
DB_PORT=5432                 # Postgres port
DB_USERNAME=postgres         # Postgres user
DB_PASSWORD=...              # Postgres password
DB_NAME=auth_db              # Database name

# Token signing (RS256)
JWT_PRIVATE_KEY=...          # Base64 PEM RSA key, 2048+ bits. Generate: ./scripts/generate-signing-key.sh
JWT_KEY_ID=...               # Key id (kid) put in every token header and the JWKS
JWT_ISSUER=auth-service      # Optional, iss claim (default: auth-service)
JWT_AUDIENCE=trading-platform  # Optional, aud claim (default: trading-platform)
JWT_ACCESS_TOKEN_TTL=900     # Optional, access token lifetime in seconds (default: 15 min)
JWT_REFRESH_TOKEN_TTL=604800 # Optional, refresh token lifetime in seconds (default: 7 days)

# App
APP_PORT=8081                # Server port
NODE_ENV=development         # Environment (development, production, test)
```

See [`.env.example`](.env.example) for a template.

### Configuration Validation

The app uses **Joi schema validation** in `src/config/validation.ts`:
- Required variables are enforced at startup
- Type validation (string, number, etc.)
- Default values for optional variables
- **App will fail to start** if validation fails with a clear error message

## Setup

### Prerequisites

- Node.js 20 LTS
- npm 11+
- Docker and Docker Compose (for containerized setup)

### Local Development

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Create `.env` file** from `.env.example`:
   ```bash
   cp .env.example .env
   # Edit .env with your values
   ```

3. **Start Postgres** (e.g., Docker):
   ```bash
   docker run -d \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -e POSTGRES_DB=auth_db \
     -p 5432:5432 \
     -v postgres_data:/var/lib/postgresql/data \
     postgres:15-alpine
   ```

4. **Load database schema**:
   ```bash
   # The schema is auto-loaded in Docker Compose
   # For local dev, manually run SQL files:
   psql -U postgres -d auth_db -f db/schema/01-users.sql
   psql -U postgres -d auth_db -f db/schema/02-auth.sql
   psql -U postgres -d auth_db -f db/schema/03-tokens.sql
   ```

5. **Start the app**:
   ```bash
   npm start
   ```

   Access at `http://localhost:8081`

### Docker Compose

1. **Start auth service and database**:
   ```bash
   docker compose up auth-db auth-service
   ```

   - `auth-db` becomes healthy after ~10s
   - `auth-service` starts after `auth-db` is healthy
   - Access at `http://localhost:8087/health`

2. **View logs**:
   ```bash
   docker compose logs -f auth-service
   docker compose logs -f auth-db
   ```

3. **Stop services**:
   ```bash
   docker compose down
   ```

## Testing

### Unit Tests

Run Jest unit tests (mocked dependencies):

```bash
npm test
```

**Test coverage**:
- `app.controller.spec.ts` - Health endpoint
- `users.service.spec.ts` - User operations
- `auth.service.spec.ts` - Auth operations

### Run Tests in Watch Mode

```bash
npm run test:watch
```

### Test Coverage Report

```bash
npm run test:cov
```

## Linting & Formatting

### ESLint

Check code quality:

```bash
npm run lint
```

### Prettier

Format code to maintain consistent style:

```bash
npm run format
```

## Development

### Start Development Server

```bash
npm run start:dev
```

Watches for file changes and auto-reloads.

### Debug Mode

```bash
npm run start:debug
```

## Building & Deployment

### Build for Production

```bash
npm run build
```

### Run Production Build

```bash
npm run start:prod
```

### Docker Build

```bash
docker build -t auth-service:latest ./app/auth-service
docker run -p 8087:8081 --env-file ./app/auth-service/.env auth-service:latest
```

## API Examples

### Register

```bash
POST /auth/register
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123"
}
```

### Login

```bash
POST /auth/login
{
  "username": "john_doe",
  "password": "SecurePass123"
}
```

### Health Check

```bash
GET /health
```

## License

UNLICENSED

