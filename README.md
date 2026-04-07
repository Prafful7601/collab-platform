# Collab Platform

A real-time collaborative editing platform built with Spring Boot, WebSockets, Redis-backed state caching, Kafka event publishing, and a clean browser UI for shared documents.

This project is already beyond a toy demo. It supports live multi-user editing, per-document collaboration rooms, version-aware edit application, saved documents, recent document discovery, and a dedicated persistence service behind the gateway.

## What It Does

- Creates shareable documents through the API gateway
- Syncs edits live over WebSockets for everyone in the same document
- Tracks document versions so clients can apply operations safely
- Stores document snapshots through a separate persistence service
- Caches document state in Redis when available, with in-memory fallback
- Publishes edit events to Kafka when the broker is running
- Shows recent documents and document stats in the UI

## Current Product Shape

Right now this is a docs-first collaboration app. The frontend is focused on writing and shared drafting, while the backend is structured more like a platform that can grow into multiple collaborative surfaces.

That makes this a strong base for:

- collaborative docs
- internal note-taking
- lightweight team knowledge sharing
- a future code editor mode

## Architecture

```text
Browser UI
  -> HTTP /documents + static UI from api-gateway
  -> WebSocket /collab for live editing

api-gateway
  -> keeps hot document state in memory + Redis
  -> applies and broadcasts edit operations
  -> forwards document snapshots to persistence-service
  -> emits document edit events to Kafka

persistence-service
  -> stores document snapshots in a database
  -> returns recent documents and saved state
```

## Repository Layout

```text
.
|-- api-gateway/            # Spring Boot gateway, WebSocket server, document API
|-- persistence-service/    # Spring Boot persistence service
|-- infra/                  # Docker Compose for Redis, Kafka, ZooKeeper, Postgres
|-- ui/                     # Static frontend served by api-gateway
|-- test-websocket.js       # Basic Node WebSocket smoke test
`-- test-fix-verification.js
```

## Tech Stack

- Java 24
- Spring Boot 3.5
- Spring Web
- Spring WebSocket
- Spring Security
- Redis
- Kafka
- H2 file database for the current persistence-service default
- Docker Compose for local infrastructure
- Plain HTML, CSS, and JavaScript frontend

## Key Backend Capabilities

### API Gateway

- Serves the UI from [`ui/`](./ui)
- Exposes document CRUD endpoints at `/documents`
- Accepts live edit operations on `/collab`
- Groups WebSocket clients by `docId`
- Applies insert and delete operations server-side
- Sends presence, snapshot, and operation events back to connected clients
- Sanitizes legacy full-content updates before saving

### Persistence Service

- Stores document snapshots independently from the live editing layer
- Returns recent documents ordered by update time
- Persists title, content, version, created time, and updated time

### State Handling

- Uses Redis as a fast snapshot cache when available
- Falls back to in-memory state if Redis or persistence-service is unavailable
- Keeps the editor usable even when some infra is down

## Frontend Features

- Calm, polished writing interface
- Shareable document links
- live connection and sync status
- word and character counts
- version and queue visibility
- recent document list
- new document creation
- multi-tab collaboration testing from the same browser

## Running Locally

### Prerequisites

- Java 24
- Maven wrapper support
- Node.js if you want to run the WebSocket test scripts
- Docker Desktop if you want the local infra services

### 1. Start the infrastructure

From the repo root:

```powershell
docker compose -f infra/docker-compose.yml up -d
```

This starts:

- Redis on `6379`
- ZooKeeper on `2181`
- Kafka on `9092`
- Postgres on `5432`

Note: the current persistence service is configured to use an H2 file database by default, so Postgres is not yet required for normal local runs.

### 2. Start the persistence service

```powershell
cd persistence-service
.\mvnw spring-boot:run
```

It runs on `http://localhost:8081`.

### 3. Start the API gateway

In another terminal:

```powershell
cd api-gateway
.\mvnw spring-boot:run
```

It runs on `http://localhost:8080` and serves the UI from the `ui/` folder.

### 4. Open the app

Visit:

```text
http://localhost:8080
```

Open the same share link in two tabs to test live collaboration.

## API Endpoints

### Document API

- `GET /documents` - list recent documents
- `POST /documents` - create a document
- `GET /documents/{id}` - fetch a document
- `PUT /documents/{id}` - update title and content

Example create request:

```json
{
  "title": "Team Notes",
  "content": "",
  "version": 0
}
```

## WebSocket Contract

Endpoint:

```text
ws://localhost:8080/collab?docId=<document-id>
```

Supported client message types today:

- `insert`
- `delete`
- `title-update`
- `update`
- `replace`

The server responds with event payloads such as:

- `presence`
- `snapshot`
- `title-update`
- `operation`

## Security Notes

- JWT-based auth endpoints exist under `/api/auth`
- document endpoints and the collaboration socket are currently open for easier local testing
- the JWT secret in `application.properties` is a development value and should be replaced before any real deployment

## Testing

Two lightweight Node scripts are included for local verification:

```powershell
node test-websocket.js
node test-fix-verification.js
```

These are smoke-test style scripts, not a complete automated test suite.

## What's Good Already

- clean separation between live collaboration and persistence
- solid direction for horizontal scaling
- graceful fallback behavior when Redis or persistence is unavailable
- practical recent-documents workflow
- UX that feels better than a typical raw demo

## What Would Make This App Better

### Product improvements

- Add proper collaborative cursors and presence avatars
- Add document ownership, sharing permissions, and invite flows
- Add revision history with restore points
- Add workspace or project organization instead of isolated docs
- Add export options like Markdown, PDF, and plain text

### Collaboration engine improvements

- Expand operational transform coverage and add more edge-case tests
- Add idempotency handling for duplicate operations
- Persist operation history, not just snapshots
- Add stronger reconnect and resync semantics

### Frontend improvements

- Add richer formatting controls and keyboard shortcuts
- Add slash commands and block-based editing
- Add comments, suggestions, and read-only review mode
- Add offline draft support with replay on reconnect

### Platform improvements

- Move persistence-service from H2 default to Postgres for production parity
- Add observability: logs, metrics, tracing, health dashboards
- Add containerized local app startup for both Spring services
- Add CI with integration tests for WebSocket, Redis, and Kafka flows

## Strong Next Step: Add A Code Editor Mode

Yes, this is a very good direction.

Instead of making the product only "Google Docs-lite", turn it into a collaborative workspace with editor modes:

- `Document mode` for rich writing and notes
- `Code mode` for source files with monospace editor behavior

The clean way to do this is not to hack code editing into the current textarea. Make the editor surface pluggable.

### Recommended approach

1. Keep the collaboration backend document-based for now.
2. Add a `documentType` or `editorMode` field such as `doc` or `code`.
3. Render different frontend editors based on that mode.
4. Start with Monaco Editor for code mode.
5. Keep the same document/session APIs, but add metadata like language, theme, and tab width.

### Why this is worth doing

- expands the app from note-taking into dev collaboration
- makes the project more unique
- opens the door to pair programming, interview rooms, and collaborative coding exercises
- gives the platform a clearer product story than "shared docs only"

### Best first version of code mode

- Monaco Editor in the UI
- syntax highlighting
- language selector
- file name support
- read-only spectator mode
- shared selection and cursor presence after the base mode is stable

## Known Gaps

- no full user-facing auth flow in the UI yet
- no role-based permissions on documents
- no production deployment setup
- no full automated integration test coverage
- Postgres is provisioned in infra but not yet the default persistence backend

## Roadmap

- Harden operation transformation and concurrency testing
- Add role-based sharing and authentication in the UI
- Introduce workspaces and folders
- Add collaborative comments and suggestion mode
- Add code editor mode with Monaco
- Add production deployment manifests and CI

## Why This Project Is Interesting

A lot of "real-time editor" projects stop at a single WebSocket demo. This one already has the beginnings of a proper distributed system:

- gateway layer
- live editing channel
- event publishing
- state caching
- persistence boundary
- a frontend that shows real product intent

That is exactly the kind of foundation worth polishing.
