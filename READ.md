# Real-Time Collaborative Editing Platform

A distributed backend system for real-time collaborative document editing built using Java and Spring Boot.

## Tech Stack

- Java (Spring Boot)
- WebSockets
- Redis (planned)
- Kafka (planned)
- PostgreSQL (planned)
- Docker

## Features

- Document creation API
- Real-time editing via WebSockets
- Document-based collaboration sessions
- Structured edit operations

## Architecture

Clients connect via WebSockets and are grouped by document sessions.

Edit operations are broadcast only to users editing the same document.

## Future Improvements

- Redis-based shared document state
- Operational Transform / CRDT conflict resolution
- Kafka event streaming
- Horizontal scaling