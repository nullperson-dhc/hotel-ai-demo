---
name: java-code-review
description: Review Java and Spring Boot changes for correctness, regressions, security, concurrency, persistence, API compatibility, and test coverage. Use when the user requests Java code review or review of backend changes; do not trigger for ordinary implementation unless review is requested.
---

# Java Code Review

Review the relevant diff and surrounding code, prioritizing actionable defects over style preferences.

- Establish the intended behavior and inspect callers, tests, configuration, schemas, and API contracts affected by the change.
- Check correctness, null handling, validation, exception mapping, transactions, authorization, data exposure, concurrency, and resource management.
- Examine JPA mappings and queries for ownership, fetch behavior, cascading, constraints, pagination, locking, and performance risks.
- Identify compatibility changes in public APIs, persisted data, configuration, and operational behavior.
- Verify that tests cover important success and failure behavior without merely mirroring implementation details.
- Report findings in severity order with tight file and line references, impact, and a concrete remediation direction.
- Do not claim a defect without a plausible failure scenario. If no actionable findings exist, say so and note meaningful residual risks or untested areas.
