# Employee Management System

A Spring Boot REST API for managing employee records, with JWT-based authentication
backed by a real user database (not hardcoded credentials).

## Tech stack

- Java 21, Spring Boot 4.1.2
- Spring Security (JWT, stateless sessions, BCrypt password hashing)
- Spring Data JPA + Hibernate
- H2 (default, in-memory) or PostgreSQL (via `DATABASE_URL` + driver/dialect overrides)
- springdoc-openapi (Swagger UI)

## Running locally

```bash
export JWT_SECRET="any-random-string-at-least-32-characters-long"
./mvnw spring-boot:run
```

`JWT_SECRET` is required — the app will not start without it, and there is no
hardcoded fallback. Generate one with:

```bash
openssl rand -base64 32
```

The app starts on port `8081` by default (override with `PORT`).

### First login

On first startup, if no users exist yet, an `admin` account is created automatically:

- If `ADMIN_DEFAULT_PASSWORD` is set, that becomes the admin password.
- Otherwise, a random password is generated and printed **once** to the startup log
  (`AdminBootstrap` logger), e.g.:
  ```
  Username: admin
  Password (shown once, not stored anywhere in plaintext): <random>
  ```
  Copy it from the log — it is not stored anywhere in plaintext and cannot be recovered
  afterward. If lost, delete the user from the database (or wipe the H2 in-memory DB by
  restarting, in dev) to trigger bootstrap again.

After logging in once, register your own account via `POST /api/auth/register` rather
than continuing to use the bootstrap admin account.

## Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | *(required, no default)* | Signing key for JWTs. At least 32 characters. |
| `ADMIN_DEFAULT_PASSWORD` | *(random, logged once)* | Password for the auto-created first admin account. |
| `PORT` | `8081` | HTTP port. |
| `DATABASE_URL` | `jdbc:h2:mem:emsdb` | JDBC URL. Set for PostgreSQL in production. |
| `DB_DRIVER` | `org.h2.Driver` | JDBC driver class. |
| `DB_USERNAME` / `DB_PASSWORD` | `sa` / *(empty)* | DB credentials. |
| `DB_DIALECT` | `org.hibernate.dialect.H2Dialect` | Hibernate dialect. |

## API

All endpoints are under `/api`. Swagger UI is available at `/swagger-ui.html` when running.

### Auth — `/api/auth` (public)

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/auth/register` | `{"username": "...", "password": "..."}` | `201` + `{token, username}`, or `409` if username taken |
| POST | `/api/auth/login` | `{"username": "...", "password": "..."}` | `200` + `{token, username}`, or `401` if invalid |

Validation: username 3–50 chars, password at least 8 chars. Invalid input returns `400`
with a `fields` map of per-field error messages.

### Employees — `/api/employees` (requires `Authorization: Bearer <token>`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/employees` | List all employees |
| GET | `/api/employees/{id}` | `404` if not found |
| POST | `/api/employees` | Body validated (`firstName`, `lastName`, `email` required; `salary` must be ≥ 0) |
| PUT | `/api/employees/{id}` | Same validation as POST |
| DELETE | `/api/employees/{id}` | `404` if not found |

Any request without a valid JWT returns `401 Unauthorized`.

## Authentication model

- Passwords are hashed with BCrypt (`PasswordEncoder`) — never stored or logged in plaintext.
- Login/registration issue a JWT (HMAC-SHA, `jjwt` library) that must be sent as
  `Authorization: Bearer <token>` on every other request.
- Sessions are stateless (`SessionCreationPolicy.STATELESS`) — no server-side session state.
- There is no hardcoded username/password anywhere in the codebase; the only
  auto-created account is the first-run `admin` bootstrap described above, and even
  that uses a random (or operator-supplied) password, never a fixed one.

## Testing

```bash
./mvnw test
```

Runs the full suite (context load + `AuthControllerTest` + `EmployeeControllerTest`)
against an in-memory H2 database via `MockMvc`.

## CORS

Allowed origins are configured explicitly in `SecurityConfig` (no wildcard, since
wildcard origins cannot be combined with `allowCredentials(true)` per the CORS spec).
Add your frontend's origin there if deploying elsewhere.
