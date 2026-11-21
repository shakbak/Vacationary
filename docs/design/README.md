# Design

Developer design: **how each service is built** (inner parts and the flow between them). Requirements stay in [docs/requirements](../requirements/README.md).

## Backend

Manager, repository, mapper, in-memory stub / H2 JDBC, HTTP pipeline. Spring wiring. No security/rate-limit extras.

Build order: recommendation → assistant → conversation → HTTP.

| File | Inner design of |
|---|---|
| [backend/overview.md](backend/overview.md) | Shared roles and package layout |
| [backend/recommendation.md](backend/recommendation.md) | Manager, catalog, mapper, generator |
| [backend/assistant.md](backend/assistant.md) | Manager, prompt, LLM client, stay-search parser, response mapper |
| [backend/conversation.md](backend/conversation.md) | Manager, conversation repo, processed-event repo |
| [backend/http-contract.md](backend/http-contract.md) | RouteHandler: validate → map → execute → respond |

## Web client

Vite React/TypeScript chat. Session, typed HTTP, chat use-case, UI. No Redux/Next.

Build order: api → session → chat → ui.

| File | Inner design of |
|---|---|
| [web/overview.md](web/overview.md) | Layers, Vite layout, proxy |
| [web/chat.md](web/chat.md) | sessionStorage, API client, send/load, outcome vs HTTP error |
| [web/ui.md](web/ui.md) | Transcript, stay cards, composer, loading/success/error, usability |

## Messaging platforms

Separate Spring process. Telegram Bot API only in v1. Calls the backend; renders outcomes as English Telegram text.

Build order: render → backend client → inbound reply manager → Telegram adapter (long poll).

| File | Inner design of |
|---|---|
| [messaging/overview.md](messaging/overview.md) | Process boundary, layers, port 8081 |
| [messaging/inbound.md](messaging/inbound.md) | Inbound reply manager, backend client, response parser, renderer |
| [messaging/telegram.md](messaging/telegram.md) | End-to-end Telegram flow, getUpdates, sendMessage |
