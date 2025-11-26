# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

summar-ai is a Spring Boot 3.4.3 application (Java 21) that aggregates productivity data from multiple OAuth-integrated tools (Google Calendar, Zoom, Jira) and generates AI-powered reports using OpenAI's GPT. The application uses OAuth2 for third-party integrations and form-based authentication for user management.

## Build and Run Commands

The project uses Maven with the wrapper script. All commands should be run from the `summar-ai/` subdirectory:

```bash
cd summar-ai

# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ClassName

# Package without running tests
./mvnw package -DskipTests

# Clean build artifacts
./mvnw clean
```

The application runs on `http://localhost:8080` by default.

## Database Setup

The application requires MySQL running on port 3307:
- Database: `summar-ai`
- Username: `root`
- Password: `root`

Connection details are in `src/main/resources/application.properties`. The application uses Hibernate with `ddl-auto=update` to manage schema automatically.

## Architecture

### Core Domain Model

The application centers around a **Tool Integration System**:

- **User**: Authenticated users (Spring Security's UserDetails implementation)
- **Tool**: Available integration tools (Google Calendar, Zoom, Jira)
- **UserTool**: Many-to-many relationship with composite key (UserToolId), stores OAuth tokens (access, refresh, expiration) and activation status
- **Report**: Generated reports from aggregated tool data

### OAuth Token Management Pattern

The critical pattern for OAuth integrations is implemented in `ToolService`:

1. User initiates OAuth flow via controller (e.g., `/oauth2/authorization/google`)
2. `SecurityConfig.customAuthorizationRequestResolver` adds `access_type=offline` to get refresh tokens
3. `OAuthController` receives callback, extracts tokens, and saves to UserTool
4. API helpers use `AuthenticatedApiCall<T>` functional interface for token-based calls
5. Each integration service implements `ToolDataService` interface with `fetchData()` method

### Service Layer Pattern

Integration services follow a consistent pattern:

1. **API Helpers** (`apihelpers/`): Low-level REST API calls (JiraApiHelper, ZoomApiHelper, GoogleCalendarApiHelper, GPTApiHelper)
2. **Integration Services** (`services/integrations/`): Implement `ToolDataService`, handle token management and data fetching
3. **Report Service**: Orchestrates parallel data collection from multiple tools using `CompletableFuture`

The `@Async` annotation on service methods enables concurrent data fetching.

### Dynamic Service Resolution

`ReportService` uses Spring's dependency injection to dynamically register all `ToolDataService` implementations:

```java
Map<String, ToolDataService> toolServiceMap;
// Maps tool names to their service implementations at runtime
```

This allows adding new integrations without modifying ReportService.

## Key Configuration Files

### application.properties

Contains OAuth2 client registrations for Google, Zoom, and Jira, plus OpenAI API key. **Note**: This file contains sensitive credentials and should not be committed in production environments.

Critical settings:
- `server.servlet.session.cookie.secure=false` - **MUST** be changed to `true` for production
- Session timeout: 30 minutes
- OAuth scopes include offline access for refresh tokens

### SecurityConfig.java

Defines security filter chain:
- Public endpoints: `/auth/login`, `/auth/register`
- Form login redirects to `/dashboard`
- OAuth2 login redirects to `/oauth2/success`
- Custom authorization request resolver for refresh token support
- Session fixation protection with `migrateSession()`
- CSRF disabled (should be enabled for production)

## Controllers and Routing

- **AuthController**: User registration and login
- **DashboardController**: Main user dashboard
- **OAuthController**: OAuth callback handler that saves tokens to UserTool
- **ReportController**: Generates reports by collecting data from activated tools
- **ToolController**: Manages user tool activation/deactivation
- **Integration Controllers** (GoogleCalendarController, JiraController, ZoomChatController): Initiate OAuth flows

## Testing

Test structure in `src/test/java/com/example/summar_ai/`:
- Currently only has `SummarAiApplicationTests.java` (basic context loading test)
- When adding tests, use Spring Boot's `@SpringBootTest` annotation
- Use H2 for test database (already in dependencies with `runtime` scope)

## Common Development Patterns

### Adding a New Integration Tool

1. Create API Helper in `apihelpers/` for REST API calls
2. Create Service implementing `ToolDataService` in `services/integrations/`
3. Add tool entry to database (Tool table)
4. Add OAuth2 client registration in `application.properties`
5. Create controller in `controllers/integrations/` to initiate OAuth flow
6. Add OAuth scope configuration in `SecurityConfig` if needed

### Working with OAuth Tokens

Access tokens are automatically refreshed when needed. All API calls should:
1. Retrieve UserTool for the current user and tool
2. Extract access token via `userTool.getAccessToken()`
3. Use the token in Authorization header as Bearer token
4. Check `expiresAt` field to determine if refresh is needed

### Report Generation Flow

1. User selects date range and timezone (stored in HttpSession)
2. `ReportController.generateReport()` retrieves active tools
3. `ReportService.collectDataFromTools()` runs all services concurrently
4. Each service returns formatted string data
5. Results are concatenated and can be sent to GPTService for summarization
6. Final report rendered via Thymeleaf template

## Project Structure

```
summar-ai/src/main/java/com/example/summar_ai/
├── apihelpers/           # External API clients
├── config/              # Spring Security and app configuration
├── controllers/         # Web endpoints
│   └── integrations/    # OAuth flow initiators
├── dto/                 # Data transfer objects
├── models/              # JPA entities
│   └── config/          # Tool-specific configuration entities
├── repositories/        # Spring Data JPA repositories
│   └── config/          # Tool-specific repositories
└── services/            # Business logic
    └── integrations/    # Tool-specific data fetchers
```

## Important Notes

- The main application is in `summar-ai/` subdirectory, not project root
- OAuth credentials in application.properties are for development only
- Cookie secure flag must be enabled before production deployment
- CSRF protection is currently disabled and should be enabled for production
- All integration services use `@Async` for parallel execution
- Composite keys (UserToolId) are used for many-to-many relationships
- Token storage uses JPA with TEXT columns for potentially long tokens
