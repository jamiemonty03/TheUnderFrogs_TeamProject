# Auth Service

NestJS-based authentication and authorization service for TheUnderFrogs trading platform. Manages user authentication, JWT token generation, and user credentials.

## Architecture

The auth-service follows a **layered architecture** pattern with clear separation of concerns:

### Module Structure

The service is organized into three core modules:

#### 1. **Auth Module** (`src/modules/auth/`)
Handles authentication logic, including user login, registration, and token validation.

- **Controller** (`auth.controller.ts`): REST endpoints
  - `POST /auth/register` - Register a new user
  - `POST /auth/login` - Authenticate user and return JWT token
- **Service** (`auth.service.ts`): Business logic
  - User registration with validation
  - Login with password verification
  - JWT token generation and validation
- **Repository** (`auth.repository.ts`): Data access
  - CRUD operations on auth records
  - Track login attempts, 2FA settings
- **Entities** (`entities/auth.entity.ts`): Database model for auth records
- **DTOs** (`dto/`): Request/response validation objects

#### 2. **Users Module** (`src/modules/users/`)
Manages user account information and lifecycle.

- **Controller** (`users.controller.ts`): User management endpoints
  - `POST /users` - Create user (201)
  - `GET /users` - List all users
  - `GET /users/:id` - Get user details
  - `PUT /users/:id` - Update user
  - `DELETE /users/:id` - Delete user (204)
- **Service** (`users.service.ts`): User operations
  - User creation with duplicate checks
  - Password hashing with bcryptjs
  - User lookups by ID, username, or email
  - User updates and deletion
- **Repository** (`users.repository.ts`): Database layer
- **Entities** (`entities/user.entity.ts`): User database model
- **DTOs** (`dto/`): User request/response objects

#### 3. **Tokens Module** (`src/modules/tokens/`)
Manages token lifecycle (refresh tokens, token revocation, expiry).

- **Service** (`tokens.service.ts`): Token operations
  - Create and store tokens
  - Token validation (existence, expiry, revocation status)
  - Token revocation for logout
  - Cleanup of expired tokens
- **Repository** (`tokens.repository.ts`): Database layer
- **Entities** (`entities/token.entity.ts`): Token database model

### Layered Design

Each module follows the same pattern:

```
Controller (HTTP)
    ↓
Service (Business Logic)
    ↓
Repository (Data Access)
    ↓
Entity (Database Model)
```

- **Controllers**: Handle HTTP requests/responses, validation via `@nestjs/class-validator`
- **Services**: Implement business rules, error handling, data transformation
- **Repositories**: Abstract database operations, use TypeORM
- **Entities**: Define database schema with TypeORM decorators

### Cross-Cutting Concerns

- **Config** (`src/config/`): Environment variables and database configuration
  - `database.config.ts`: TypeORM connection setup
  - `validation.ts`: Joi schema for required environment variables
- **Common** (`src/common/`): Shared utilities
  - `exceptions/`: Custom exception classes
- **Health Check** (`src/app.controller.ts`): `GET /health` endpoint for monitoring

## Database

### Postgres 15 (Alpine)

**Schema** (`db/schema/`):
1. **01-users.sql** - Users table with username, email, password, role, is_active
2. **02-auth.sql** - Auth records table for login tracking and 2FA settings
3. **03-tokens.sql** - Refresh tokens and token revocation tracking

**Seed Data**:
- 2 test users (admin, testuser) with hashed passwords
- Corresponding auth and token records

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

# JWT
JWT_SECRET=...               # Min 32 characters (MUST match across all services)
JWT_EXPIRATION=86400000      # Token lifetime in milliseconds (default: 24h)

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

