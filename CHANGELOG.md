# Changelog

All notable changes to this project will be documented in this file.

## [v2.0.0] - 2026-07-27

### Features

- Added Spring Security authentication with login and registration flows.
- Added user-scoped task access so authenticated users can only access their own tasks.
- Added HTMX + Thymeleaf web interface for task management.
- Added task editing from the web UI.
- Added due dates and reminder times for tasks.
- Added recurring tasks with daily, weekly, and monthly recurrence.
- Added Schedule page with filters for today, tomorrow, next 7 days, later, and tasks without dates.
- Added Notifications page for upcoming, queued, sent, and failed task reminders.
- Added Markdown rendering support for Liyz chat responses.
- Added returned `conversationId` flow in the web chat.
- Removed the temporary fixed development conversation fallback.

### Notifications

- Added RabbitMQ integration for asynchronous task notification delivery.
- Added Resend integration for reminder emails.
- Added notification scheduler for due reminders.
- Added notification publisher and consumer flow.
- Added retry and disable actions for task reminders.
- Added email notification status tracking with pending, queued, sent, and failed states.

### AI Improvements

- Improved Liyz tool behavior for task creation, update, completion, deletion, search, reminders, and recurrence.
- Added task operations by title when a clear unique match exists.
- Improved AI task flow so newly created and updated tasks are reflected in the UI without manual refresh.
- Improved prompts to avoid unsafe identity assumptions based only on the user's name.
- Improved task description constraints to prevent AI-generated descriptions from exceeding application limits.
- Improved language behavior so Liyz follows the user's latest message language.
- Added current date and timezone context for reminder creation.
- Added defensive AI fallback for malformed tool calls from OpenAI-compatible providers.

### Frontend

- Added minimalist dark UI inspired by a product dashboard style.
- Added sidebar navigation for My Tasks, Schedule, and Notifications.
- Added Bootstrap Icons for navigation and logout actions.
- Added Liyz logo and favicon.
- Added minimalist footer with product links and copyright.
- Added interactive Liyz assistant panel with robot icon styling.
- Added sort toggle for recent tasks.
- Added responsive layout improvements for desktop and mobile.
- Added dedicated visual states for pending, completed, recurring, and reminder-enabled tasks.

### Database

- Added Flyway migrations for task notification fields.
- Added Flyway migration to expand task descriptions.
- Added Flyway migration for task recurrence.

### Tests

- Added and updated unit tests for authenticated task access.
- Added tests for task creation, updates, deletion, recurrence, notifications, chat history, Redis memory, and prompt injection protection.
- Current test suite: 48 passing tests.

### Infrastructure

- Added RabbitMQ service to Docker Compose.
- Added notification-related application properties.
- Added async and scheduled processing support for notification workflows.

### Fixes

- Fixed HTMX task actions after AI-triggered task list refreshes.
- Fixed login and register logo loading by allowing public access to static image assets.

## [v1.2.0] - 2026-07-17

### Features

- Added Redis-backed conversational memory for Liyz using Spring AI chat memory.
- Added persistent AI chat history using PostgreSQL.
- Added `conversationId` support to AI chat requests and responses.
- Added chat conversation and chat message JPA entities.
- Added Flyway database migrations.
- Added Redis service to Docker Compose.
- Added configurable chat memory TTL.
- Added temporary fixed development conversation fallback for local testing.

### AI Improvements

- Improved modular system prompts for task creation behavior.
- Added stronger prompt injection detection for Portuguese and English attacks.
- Added safeguards against revealing internal prompts, tools, configuration, and hidden instructions.
- Improved identity instructions so Liyz does not confuse the user's name with her own name.

### Infrastructure

- Added Spring Boot Flyway starter integration.
- Added Redis configuration properties.
- Disabled Redis repository scanning because the project uses `StringRedisTemplate`.
- Switched Hibernate schema handling to validation mode with Flyway-managed schema.

### Fixes

- Fixed duplicate task creation guidance by requiring one task creation tool call per single task request.
- Fixed Redis chat memory repository implementation.
- Fixed missing `ObjectMapper` bean issue by creating the mapper inside the Redis memory repository.
- Fixed local Redis connection setup through Docker Compose.
- Fixed Flyway not running by replacing raw `flyway-core` usage with `spring-boot-starter-flyway`.

## [v1.0.0] - 2026-07-13 (Initial Release)

### Endpoints

- GET /api/tasks — List all tasks
- GET /api/tasks/{id} — Get a task by ID
- POST /api/tasks — Create a new task
- PATCH /api/tasks/{id} — Update a task completion status
- DELETE /api/tasks/{id} — Delete a task by ID

### Features

- Task CRUD API
- Request, Response, and Status DTOs
- Pagination support
- PostgreSQL integration
- OpenAPI/Swagger documentation
- Global exception handling with `@RestControllerAdvice`
- Standardized error responses using `ProblemDetail`
