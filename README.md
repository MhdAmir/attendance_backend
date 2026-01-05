# Eros Attendance API - Authentication with PASETO

This Spring Boot application implements a complete authentication system using PASETO (Platform-Agnostic Security Tokens).

## Features

✅ User registration and login
✅ PASETO token-based authentication
✅ Access and refresh token support
✅ Protected API endpoints
✅ OpenAPI/Swagger documentation
✅ Best practices for security

## Architecture

### Components Created

1. **Entities**
   - `User` - User entity with JPA annotations

2. **DTOs**
   - `RegisterRequest` - Registration request
   - `LoginRequest` - Login request
   - `RefreshTokenRequest` - Token refresh request
   - `AuthResponse` - Authentication response with tokens
   - `UserResponse` - User information response
   - `ApiResponse<T>` - Generic API response wrapper

3. **Repository**
   - `UserRepository` - JPA repository for user operations

4. **Services**
   - `PasetoService` - PASETO token generation and validation
   - `AuthService` - Authentication business logic

5. **Controllers**
   - `AuthController` - Public authentication endpoints
   - `UserController` - Protected user endpoints

6. **Security**
   - `PasetoAuthenticationFilter` - Validates PASETO tokens
   - `SecurityConfig` - Security and CORS configuration

7. **Configuration**
   - `OpenApiConfig` - Swagger/OpenAPI setup
   - `GlobalExceptionHandler` - Global error handling

## API Endpoints

### Public Endpoints (No Authentication Required)

#### 1. Register User
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "fullName": "John Doe"
}
```

#### 2. Login
```
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "john_doe",
  "password": "password123"
}
```

#### 3. Refresh Token
```
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "v2.local.xxx..."
}
```

### Protected Endpoints (Require Authentication)

All protected endpoints require the Authorization header:
```
Authorization: Bearer v2.local.xxx...
```

#### 4. Get Current User
```
GET /api/users/me
Authorization: Bearer <access_token>
```

#### 5. Get All Users
```
GET /api/users
Authorization: Bearer <access_token>
```

#### 6. Get User by ID
```
GET /api/users/{id}
Authorization: Bearer <access_token>
```

## Token Types

### Access Token
- **Purpose**: Authenticate API requests
- **Expiration**: 15 minutes (900 seconds)
- **Usage**: Include in Authorization header for protected endpoints

### Refresh Token
- **Purpose**: Generate new access/refresh tokens
- **Expiration**: 7 days (604800 seconds)
- **Usage**: Call `/api/auth/refresh` when access token expires

## Configuration

Configuration is in `application.properties`:

```properties
# PASETO Configuration
paseto.secret.key=ThisIsASecretKeyForPasetoThatMustBe32BytesLongMinimum
paseto.access.token.expiration=900
paseto.refresh.token.expiration=604800
```

⚠️ **Important**: Change the `paseto.secret.key` in production!
Generate a secure key: `openssl rand -base64 32`

## Running the Application

1. **Start PostgreSQL** (ensure database exists)
   ```bash
   # Database configuration in application.properties:
   # URL: jdbc:postgresql://localhost:5432/eros_attendance_db
   # Username: eros
   # Password: EROS1NASIONAL
   ```

2. **Build and Run**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

3. **Access Swagger UI**
   ```
   http://localhost:8080/swagger-ui.html
   ```

## API Documentation

The API includes comprehensive OpenAPI/Swagger documentation:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

You can test all endpoints directly from Swagger UI, including authentication.

## Security Best Practices

✅ Passwords hashed with BCrypt
✅ PASETO tokens (more secure than JWT)
✅ Separate access and refresh tokens
✅ Token validation on every protected request
✅ CORS configuration
✅ Input validation with Bean Validation
✅ Global exception handling

## Testing with cURL

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "password123"
  }'
```

### Access Protected Endpoint
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer v2.local.xxx..."
```

## Response Format

All responses follow this format:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

Error responses:
```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

## Dependencies

- Spring Boot 4.0.1
- Spring Data JPA
- Spring Validation
- PostgreSQL Driver
- jPaseto 0.7.0
- SpringDoc OpenAPI 2.3.0
- BCrypt (Spring Security Crypto)
