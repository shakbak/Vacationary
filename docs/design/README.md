# Design

Developer design: **how each backend domain is built** (inner parts and the flow between them). Requirements stay in [docs/requirements](../requirements/README.md).

Manager, repository, mapper, in-memory stub, HTTP pipeline. Spring wiring. No security/rate-limit extras.

Build order: recommendation → assistant → conversation → HTTP.

| File | Inner design of |
|---|---|
| [backend/overview.md](backend/overview.md) | Shared roles and package layout |
| [backend/recommendation.md](backend/recommendation.md) | Manager, catalog, mapper, generator |
| [backend/assistant.md](backend/assistant.md) | Manager, prompt, LLM client, stay-search parser, response mapper |
| [backend/conversation.md](backend/conversation.md) | Manager, conversation repo, processed-event repo |
| [backend/http-contract.md](backend/http-contract.md) | RouteHandler: validate → map → execute → respond |

Messaging and web design are not in this folder yet.
