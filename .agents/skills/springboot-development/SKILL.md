---
name: springboot-development
description: Build or modify the Java Spring Boot backend for this project, including APIs, services, persistence, validation, security, configuration, and tests. Use for work under backend/; do not use for frontend-only tasks.
---

# Spring Boot Development

Implement backend changes consistently with the repository's existing Java and Spring Boot conventions.

- Inspect the current build, package layout, configuration, and established patterns before choosing libraries or structure.
- Keep controllers focused on HTTP concerns, application services on use cases, and domain logic close to the model that owns it.
- Validate inputs, use explicit transaction boundaries, and return stable, documented error responses.
- Protect credentials and environment-specific settings; never commit secrets.
- Design persistence mappings and schema changes together, accounting for indexes, constraints, concurrency, and migrations.
- Add proportionate unit or integration tests for observable behavior and failure paths.
- Update relevant API, database, or technical documentation when contracts or architecture change.
- Run the repository's formatting, build, and test commands before completion when available.
