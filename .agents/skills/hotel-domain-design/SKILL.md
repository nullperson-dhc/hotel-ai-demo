---
name: hotel-domain-design
description: Design hotel-management domains, requirements, business rules, entities, workflows, and data models. Use for hotel booking, inventory, rooms, rates, guests, stays, payments, and related domain analysis; do not use for generic application architecture without hotel-domain decisions.
---

# Hotel Domain Design

Model hotel concepts with explicit terminology, ownership, lifecycle, and invariants.

- Clarify actors, business goals, scope, assumptions, and unresolved decisions before fixing a domain model.
- Distinguish room types, physical rooms, sellable inventory, rate plans, reservations, stays, guests, orders, payments, refunds, and cancellations.
- Describe important state transitions and enforce rules at the boundary that owns them.
- Treat availability, pricing, taxes, time zones, cancellation policies, overbooking, and idempotency as first-class concerns when relevant.
- Keep requirements, domain objects, database structures, and APIs traceable to one another.
- Record material assumptions and alternatives in the appropriate files under `docs/`.
- Avoid inventing business policies; when a missing policy materially changes the design, surface the decision clearly.
