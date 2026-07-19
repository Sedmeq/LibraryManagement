# Library Management API

A RESTful CRUD API for managing books, authors, and library members — built with Spring Boot, Spring Data JPA, and MySQL.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17 |
| Framework | Spring Boot 4.x |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8 (prod), H2 in-memory (test) |
| Validation | Jakarta Bean Validation |
| Security | Spring Security 6 + JWT (JJWT 0.12.x), BCrypt |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Gradle |

---

## Prerequisites

- Java 17+
- MySQL 8+ running locally
- Gradle (or use the included `gradlew` wrapper)

---

## Getting Started

### 1. Create the database

```sql
CREATE DATABASE testdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure credentials

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/testdb
    username: root
    password: your_password
```

### 3. Run the application

```bash
./gradlew bootRun
```

The app starts on **http://localhost:8080**.  
Schema is created/updated automatically via `ddl-auto: update`.

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON spec:

```
http://localhost:8080/v3/api-docs
```

---

## Authentication (JWT)

The API uses stateless JWT authentication. Register or log in to receive an `accessToken`,
then send it as `Authorization: Bearer <token>` on every subsequent request.

### Auth — `/api/v1/auth` (public)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register a new account (always created with role `USER`) |
| POST | `/api/v1/auth/login` | Log in, returns a JWT |

**Register / Login request body:**
```json
{ "username": "john", "email": "john@example.com", "password": "secret123" }
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "username": "john",
  "role": "USER"
}
```

A default admin account is seeded automatically on first startup (see `config/DataSeeder.java`):

```
username: admin
password: Admin123!
```

### Role-based access

| Endpoint | USER | ADMIN |
|---|---|---|
| `GET /api/v1/{books,authors,members}/**` | ✅ | ✅ |
| `POST/PUT/DELETE /api/v1/{books,authors,members}/**` | ❌ (403) | ✅ |
| `GET /api/v1/users/me` | ✅ | ✅ |
| `GET /api/v1/admin/**` | ❌ (403) | ✅ |

### Auth error responses

| Situation | Status | Meaning |
|---|---|---|
| No token / invalid token / expired token | **401 Unauthorized** | Caller could not be authenticated |
| Valid token but wrong role | **403 Forbidden** | Caller is authenticated but not allowed |
| Wrong username/password on `/auth/login` | **401 Unauthorized** | Credentials invalid |

Both cases return the same `ErrorResponseDto` JSON shape used across the API (see *Error Responses* below).

### Token expiration

Tokens are signed HS256 JWTs with an `exp` claim controlled by `jwt.expiration` (ms) in
`application.yml` (default: 1 hour / `3600000`). Expired tokens are rejected by
`JwtAuthenticationFilter` and result in a 401 with the message `"Token vaxtı bitib. Yenidən daxil olun."`
Configure both `jwt.secret` and `jwt.expiration` via environment variables (`JWT_SECRET`, `JWT_EXPIRATION`)
in production instead of the checked-in defaults.

---

## Endpoints

### Authors — `/api/v1/authors`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/authors` | Create author |
| GET | `/api/v1/authors/{id}` | Get author by id |
| GET | `/api/v1/authors` | List authors (paginated) |
| PUT | `/api/v1/authors/{id}` | Update author |
| DELETE | `/api/v1/authors/{id}` | Delete author |

### Books — `/api/v1/books`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/books` | Create book |
| GET | `/api/v1/books/{id}` | Get book by id |
| GET | `/api/v1/books` | List books (paginated) |
| PUT | `/api/v1/books/{id}` | Update book |
| DELETE | `/api/v1/books/{id}` | Delete book |

### Members — `/api/v1/members`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/members` | Create member |
| GET | `/api/v1/members/{id}` | Get member by id |
| GET | `/api/v1/members` | List members (paginated) |
| PUT | `/api/v1/members/{id}` | Update member |
| DELETE | `/api/v1/members/{id}` | Delete member |

---

## Pagination & Sorting

All list endpoints accept the following query parameters:

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-indexed) |
| `size` | `10` | Items per page (max: 100) |
| `sort` | `id,asc` | Field and direction |

**Examples:**
```
GET /api/v1/books?page=0&size=5&sort=title,asc
GET /api/v1/members?page=1&size=10&sort=membershipDate,desc
GET /api/v1/books?sort=publicationYear,desc&sort=title,asc
```

**Allowed sort fields per resource:**

| Resource | Fields |
|---|---|
| Authors | `id`, `fullName`, `biography` |
| Books | `id`, `title`, `isbn`, `publicationYear` |
| Members | `id`, `fullName`, `email`, `membershipDate` |

---

## Error Responses

All errors follow a consistent structure:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Book tapılmadı, id = 99",
  "path": "/api/v1/books/99"
}
```

Validation errors include a `validationErrors` map:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validasiya xətası",
  "validationErrors": {
    "isbn": "isbn formatı yanlışdır",
    "title": "title boş ola bilməz"
  }
}
```

| Scenario | HTTP Status |
|---|---|
| Resource not found | 404 Not Found |
| Duplicate email / ISBN | 409 Conflict |
| Validation failure | 400 Bad Request |
| Invalid sort field | 400 Bad Request |
| Malformed JSON | 400 Bad Request |
| Unexpected error | 500 Internal Server Error |

---

## Running Tests

Tests use an H2 in-memory database — no MySQL required.

```bash
# Run all tests
./gradlew test

# Run only service unit tests
./gradlew test --tests "org.example.librarymanagement.service.*ImplTest"

# Run only integration tests
./gradlew test --tests "org.example.librarymanagement.service.*IntegrationTest"
```

Test report is generated at:
```
build/reports/tests/test/index.html
```

---

## Project Structure

```
src/
├── main/java/.../librarymanagement/
│   ├── config/         # OpenAPI + Security configuration, admin data seeder
│   ├── controller/     # REST controllers (incl. AuthController, AdminController)
│   ├── dto/            # Request / Response DTOs
│   ├── entity/         # JPA entities (incl. User, Role)
│   ├── exception/      # Custom exceptions + GlobalExceptionHandler
│   ├── repository/     # Spring Data JPA repositories
│   ├── security/       # JWT filter/service, UserDetailsService, 401/403 handlers
│   └── service/        # Service interfaces + implementations
└── test/
    ├── java/.../service/   # Unit tests (Mockito) + Integration tests (H2)
    └── resources/
        └── application-test.yml
```
