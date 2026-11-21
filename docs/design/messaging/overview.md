# Messaging platforms service — design

Separate Java Spring process at `services/messaging` (port **8081**). Telegram in, backend HTTP out. This process never runs the LLM, never stores canonical history, and never searches stays.

v1 implements **exactly one** platform: Telegram Bot API. A later platform is a new adapter here plus a new `channel` on backend calls — not a change to assistant or recommendation.

Build order: **render → backend client → inbound reply manager → Telegram adapter (poller)**.

## How the service is built

Same roles as the backend: `Bl*` DTOs, `I*` ports, managers that depend only on interfaces, infra adapters. Long polling is the inbound edge; there is no webhook in v1.

| Role | What it is | What it is not |
|---|---|---|
| **`BlInboundMessage`** | channel, external user id, text, event id | Telegram `Update` JSON |
| **`BlTurnResult`** | backend outcome (`recommend` / `clarify` / `refuse_scope` / `fail`), text, stays, duplicate | Bot API payload |
| **`BlOutboundMessage`** | recipient id + rendered English body | Bot API payload |
| **`IInboundReplyManager`** | One inbound message → at most one outbound reply | HTTP, Telegram JSON, `sendMessage` |
| **`IBackendClient`** | `POST /turns` | JSON parse, assistant logic |
| **`TurnResponseParser`** | Backend `/turns` JSON → `BlTurnResult` | HTTP |
| **`IReplyRenderer`** | Outcome → English body (5 stays when recommend) | HTTP |
| **`IMessageSender`** | Deliver one text to a platform user | Backend |
| **Telegram adapter** | Long-poll `getUpdates`, parse private text, send via `sendMessage` | Shared core |

## Folder layout

```
services/messaging
  src/main/java/com/vacationary/messaging/
    MessagingApplication
    bootstrap/            bind interfaces → implementations
    core/interfaces/
    dtos/
    domain/inbound/       InboundReplyManager
    domain/render/        ReplyRenderer, stay-type labels, strip tables
    infra/backend/        HttpBackendClient, TurnResponseParser
    infra/telegram/       parser, Bot API client, update handler, poller
```

No `http/` webhook controller in v1. Spring still listens on 8081 so the process is a normal Boot app; inbound is the poller, not a route.

## Layer graph

```
TelegramUpdatePoller              retry loop
  └── TelegramUpdateHandler.poll  getUpdates + offset + each Update
        TelegramBotClient         getUpdates / sendMessage
        IInboundParser            Update JSON → 0..1 BlInboundMessage
        IInboundReplyManager      text → Optional<BlOutboundMessage>
          ├── IBackendClient      POST localhost:8080/turns  channel=telegram
          │     └── TurnResponseParser   JSON body → BlTurnResult
          └── IReplyRenderer      English body
```

`duplicate=true` from the backend → **do not send** (M-10). Backend HTTP/network failure → still try to send a retry message on Telegram (M-09). If `sendMessage` fails, try one short retry line; if that fails too, stop. A later poll of the same `update_id` is skipped as duplicate (M-11).

**Telegram message path (step by step):** [telegram.md](telegram.md).

Inner docs:

- [inbound.md](inbound.md) — inbound reply manager, backend client, renderer
- [telegram.md](telegram.md) — Bot API long poll, parse, `sendMessage`
