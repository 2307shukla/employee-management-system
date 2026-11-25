# Employee Management System

A Spring Boot REST API for managing employee records, with JWT-based authentication
backed by a real user database (not hardcoded credentials).

**Live app:** https://ems-frontend-88e9.onrender.com &nbsp;·&nbsp; **API docs:** https://employee-management-system-bwvw.onrender.com/swagger-ui.html
(Free hosting: the first request after a quiet spell can take up to a minute.)

## At a glance

- Create, view, update and delete employee records through a REST API.
- Sign-in with tokens; passwords are stored hashed and there are no built-in default logins.
- Role-based access: read-only employees and administrators.
- Built with Java, Spring Boot and Spring Security, on PostgreSQL (in-memory database for local use).
- Has a companion React app: [ems-frontend](https://github.com/2307shukla/ems-frontend).

## Tech stack

- Java 21, Spring Boot 4.1.2
- Spring Security (JWT, stateless sessions, BCrypt password hashing)
- Spring Data JPA + Hibernate
- H2 (default, in-memory, dev only) or PostgreSQL (via `DATABASE_URL` + driver/dialect overrides)
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
| `CORS_ALLOWED_ORIGINS` | the deployed frontend and `http://localhost:3000` | Comma-separated browser origins allowed to call the API. |
| `LAZY_INIT` | `true` | Lazy bean creation (faster cold start). Set `false` to fail fast on startup errors. |
| `SPRING_PROFILES_ACTIVE` | *(none)* | `dev` enables the H2 console and SQL logging. Never set in production. |

## Production deployment

- **Database:** the default H2 database is in-memory and loses all data on restart. Set `DATABASE_URL` (`jdbc:postgresql://host:5432/db`), `DB_DRIVER=org.postgresql.Driver`, `DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect`, `DB_USERNAME` and `DB_PASSWORD`.
- **Secrets:** `JWT_SECRET` and `ADMIN_DEFAULT_PASSWORD` live only in the platform's environment settings. Generate the secret with `openssl rand -base64 48`; changing it logs everyone out.
- **Health check:** point the platform's health check at `/actuator/health` (public, no details exposed).
- **CORS:** set `CORS_ALLOWED_ORIGINS` to the exact frontend origin(s). Nothing else can call the API from a browser.
- **Container:** multi-stage image running as a non-root user, with a `HEALTHCHECK` and container-aware JVM memory settings (`JAVA_OPTS` to override).
- **Hardening:** the H2 console is off outside the `dev` profile, clickjacking protection is on, SQL logging is off, and `X-Forwarded-*` headers are honoured behind the platform proxy.
- **CI:** every push runs `mvn verify` and builds the Docker image.
- **Next step for real data:** schema is managed by Hibernate `update`; add Flyway migrations before storing anything you cannot recreate.

## Startup time on constrained hosts

Bean creation is lazy (`spring.main.lazy-initialization=true`) to keep cold-start time
down on small/free-tier instances, and the datasource connection pool fails fast
(`spring.datasource.hikari.connection-timeout=10000`) instead of hanging silently if the
database is unreachable. If a deploy's logs stop right after "Finished Spring Data
repository scanning" with no further output before the platform's own deploy timeout
fires, that's consistent with the app still being mid-startup when it got killed, not a
crash — check whether the configured database is reachable and how long it takes to
accept a first connection.

## API

All endpoints are under `/api`. Swagger UI is available at `/swagger-ui.html` when running.

### Auth — `/api/auth` (public)

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/auth/register` | `{"username": "...", "password": "..."}` | `201` + `{token, username, role}`, or `409` if username taken |
| POST | `/api/auth/login` | `{"username": "...", "password": "..."}` | `200` + `{token, username, role}`, or `401` if invalid |

Validation: username 3–50 chars, password at least 8 chars. Invalid input returns `400`
with a `fields` map of per-field error messages. Self-registration always creates an
`EMPLOYEE`-role account — there is no way to self-assign `ADMIN`.

### Employees — `/api/employees` (requires `Authorization: Bearer <token>`)

| Method | Path | Role required | Notes |
|---|---|---|---|
| GET | `/api/employees` | any authenticated user | Paginated + searchable: `?page=0&size=20&sort=firstName,asc&name=ali&departmentId=1&designationId=2&minSalary=50000&maxSalary=90000` (all search params optional, combine with AND) |
| GET | `/api/employees/{id}` | any authenticated user | `404` if not found |
| POST | `/api/employees` | `ADMIN` | Body validated (`firstName`, `lastName`, `email` required; `salary` must be ≥ 0); `403` for non-admins |
| PUT | `/api/employees/{id}` | `ADMIN` | Same validation as POST; `404` if not found; `403` for non-admins |
| DELETE | `/api/employees/{id}` | `ADMIN` | `204` on success, `404` if not found, `403` for non-admins |

Any request without a valid JWT returns `401 Unauthorized`. An authenticated request
missing the required role returns `403 Forbidden`.

`GET /api/employees` returns a `PagedModel`:
```json
{
  "content": [ { "id": 1, "firstName": "...", ... } ],
  "page": { "size": 20, "number": 0, "totalElements": 42, "totalPages": 3 }
}
```

An employee has: `employeeCode`, `firstName`, `lastName`, `email`, `mobileNo`, `dob`,
`gender` (`MALE`/`FEMALE`/`OTHER`), `department` (nested `Department`), `designation`
(nested `Designation`), `status` (`ACTIVE`/`INACTIVE`, defaults to `ACTIVE`), `salary`,
`joiningDate`. `POST`/`PUT` bodies use `departmentId`/`designationId` (plain numbers,
not nested objects) — an unknown id returns `404`.

### Departments — `/api/departments`, Designations — `/api/designations`

Same shape and role rules as employees: any authenticated user can read, only `ADMIN`
can create/update/delete. `Department` is `{id, name}`; `Designation` is `{id, title}`.
Both names are unique.

## Authentication & authorization model

- Passwords are hashed with BCrypt (`PasswordEncoder`) — never stored or logged in plaintext.
- Login/registration issue a JWT (HMAC-SHA, `jjwt` library) that must be sent as
  `Authorization: Bearer <token>` on every other request.
- Sessions are stateless (`SessionCreationPolicy.STATELESS`) — no server-side session state.
- There is no hardcoded username/password anywhere in the codebase; the only
  auto-created account is the first-run `admin` bootstrap described above, and even
  that uses a random (or operator-supplied) password, never a fixed one.
- Two roles: `ADMIN` (full access) and `EMPLOYEE` (read-only on `/api/employees`).
  The first bootstrapped account is always `ADMIN`; every self-registered account is
  `EMPLOYEE`.
- `/error` is explicitly `permitAll()`. This isn't a security hole: Spring's default
  `AccessDeniedHandler` calls `response.sendError(403)`, which makes the servlet
  container internally forward the request to `/error` as a fresh dispatch. That
  forward re-enters this same security filter chain with an anonymous context: without
  permitting it, that second pass fails *its own* authentication check and produces a
  401 that silently overwrites the correct 403. Permitting `/error` just lets the
  original status code reach the client unchanged.

## Testing

```bash
./mvnw test
```

Runs the full suite (context load + `AuthControllerTest` + `EmployeeControllerTest`)
against an in-memory H2 database via `MockMvc`, including role-based access checks and
pagination.

Note: `MockMvc` doesn't exercise a real servlet container, so it can't reproduce the
`/error`-forward interaction described above — that was only caught by testing against
a live-running instance over real HTTP, not by the test suite alone.

## CORS

Allowed origins are configured explicitly in `SecurityConfig` (no wildcard, since
wildcard origins cannot be combined with `allowCredentials(true)` per the CORS spec).
Add your frontend's origin there if deploying elsewhere.
