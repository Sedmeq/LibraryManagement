# Library Management API

A RESTful CRUD API for managing books, authors, and library members — built with Spring Boot, Spring Security 6, JWT, and MySQL.

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
| Testing | JUnit 5, Mockito, MockMvc, H2 |

---

## Prerequisites

- Java 17+
- MySQL 8+ running locally
- Gradle (or use the included `gradlew` wrapper)

---

## Environment Variables

The application **will not start** unless these environment variables are set.

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | **Yes** | Base64-encoded HMAC-SHA key (minimum 32 bytes). No default — startup fails if absent or blank. |
| `DB_USERNAME` | **Yes** | MySQL username |
| `DB_PASSWORD` | **Yes** | MySQL password |
| `DB_URL` | No | JDBC URL (default: `jdbc:mysql://localhost:3306/testdb`) |
| `JWT_EXPIRATION` | No | Token lifetime in milliseconds (default: `3600000` = 1 hour) |

### Generating a JWT secret

```bash
# Linux / macOS
openssl rand -base64 48

# Windows PowerShell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

### Setting variables (Linux / macOS)

```bash
export JWT_SECRET="<output from above>"
export DB_USERNAME="root"
export DB_PASSWORD="your_password"
```

### Setting variables (Windows PowerShell)

```powershell
$env:JWT_SECRET  = "<output from above>"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
```

---

## Getting Started

### 1. Create the database

```sql
CREATE DATABASE testdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Set environment variables

See the [Environment Variables](#environment-variables) section above.

### 3. Run the application

```bash
./gradlew bootRun
```

The app starts on **http://localhost:8080**.  
Schema is created/updated automatically via `ddl-auto: update`.

---

## API Documentation

Swagger UI:

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

### Auth endpoints — `/api/v1/auth` (public)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register a new account (role: `USER`) |
| POST | `/api/v1/auth/login` | Log in, returns a JWT |

**Register request body:**
```json
{
  "username": "john",
  "email": "john@example.com",
  "password": "secret123"
}
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

A default admin account is seeded automatically on first startup:

```
username: admin
password: Admin123!
```

### Role-based access

| Endpoint | USER | ADMIN |
|---|---|---|
| `GET /api/v1/{books,authors,members}/**` | ✅ | ✅ |
| `POST/PUT/DELETE /api/v1/{books,authors,members}/**` | ❌ 403 | ✅ |
| `GET /api/v1/users/me` | ✅ | ✅ |
| `GET /api/v1/admin/**` | ❌ 403 | ✅ |

### Auth error responses

| Situation | Status |
|---|---|
| No token / invalid token / expired token | 401 Unauthorized |
| Valid token but insufficient role | 403 Forbidden |
| Wrong username or password on login | 401 Unauthorized |
| Duplicate username / email / ISBN | 409 Conflict |

---

## Endpoints

### Authors — `/api/v1/authors`

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/authors` | ADMIN |
| GET | `/api/v1/authors/{id}` | USER, ADMIN |
| GET | `/api/v1/authors` | USER, ADMIN |
| PUT | `/api/v1/authors/{id}` | ADMIN |
| DELETE | `/api/v1/authors/{id}` | ADMIN |

### Books — `/api/v1/books`

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/books` | ADMIN |
| GET | `/api/v1/books/{id}` | USER, ADMIN |
| GET | `/api/v1/books` | USER, ADMIN |
| PUT | `/api/v1/books/{id}` | ADMIN |
| DELETE | `/api/v1/books/{id}` | ADMIN |

### Members — `/api/v1/members`

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/members` | ADMIN |
| GET | `/api/v1/members/{id}` | USER, ADMIN |
| GET | `/api/v1/members` | USER, ADMIN |
| PUT | `/api/v1/members/{id}` | ADMIN |
| DELETE | `/api/v1/members/{id}` | ADMIN |

---

## Pagination & Sorting

All list endpoints accept:

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-indexed) |
| `size` | `10` | Items per page (max: 100) |
| `sort` | `id,asc` | Field and direction |

```
GET /api/v1/books?page=0&size=5&sort=title,asc
GET /api/v1/members?page=1&size=10&sort=membershipDate,desc
```

Allowed sort fields:

| Resource | Fields |
|---|---|
| Authors | `id`, `fullName`, `biography` |
| Books | `id`, `title`, `isbn`, `publicationYear` |
| Members | `id`, `fullName`, `email`, `membershipDate` |

---

## Error Responses

All errors share the same structure:

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
    "isbn": "isbn formatı yanlışdır"
  }
}
```

| Scenario | Status |
|---|---|
| Resource not found | 404 |
| Duplicate email / ISBN | 409 |
| Validation failure | 400 |
| Invalid sort field | 400 |
| Malformed JSON | 400 |
| Unexpected error | 500 |

---

## Running Tests

Tests use an H2 in-memory database — **no MySQL or environment variables required**.

```bash
# Run all tests
./gradlew test

# Run only service unit tests
./gradlew test --tests "org.example.librarymanagement.service.*ImplTest"

# Run only service integration tests
./gradlew test --tests "org.example.librarymanagement.service.*IntegrationTest"

# Run security integration tests
./gradlew test --tests "org.example.librarymanagement.security.SecurityIntegrationTest"
```

Test report:

```
build/reports/tests/test/index.html
```

---

## Project Structure

```
src/
├── main/java/.../librarymanagement/
│   ├── config/         # SecurityConfig, OpenApiConfig, DataSeeder, JwtSecretValidator
│   ├── controller/     # REST controllers
│   ├── dto/            # Request / Response DTOs
│   ├── entity/         # JPA entities (User, Role, Author, Book, Member)
│   ├── exception/      # Custom exceptions + GlobalExceptionHandler
│   ├── repository/     # Spring Data JPA repositories
│   ├── security/       # JwtService, JwtAuthenticationFilter, UserDetailsService, 401/403 handlers
│   └── service/        # Service interfaces + implementations
└── test/
    ├── java/.../
    │   ├── security/   # SecurityIntegrationTest (MockMvc + real Spring Security)
    │   └── service/    # Unit tests (Mockito) + Integration tests (H2)
    └── resources/
        └── application-test.yml
```
