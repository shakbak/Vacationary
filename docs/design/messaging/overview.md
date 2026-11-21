# Messaging platforms service — design

Separate Java Spring process at `services/messaging` (port **8081**). WhatsApp in, backend HTTP out. This process never runs the LLM, never stores canonical history, and never searches stays.

v1 implements **exactly one** platform: WhatsApp Cloud API. A later platform is a new adapter here plus a new `channel` on backend calls — not a change to assistant or recommendation.

Build order: **render → backend client → inbound reply manager → WhatsApp adapter → webhook HTTP**.

## How the service is built

Same roles as the backend: `Bl*` DTOs, `I*` ports, managers that depend only on interfaces, infra adapters, thin HTTP.

| Role | What it is | What it is not |
|---|---|---|
| **`BlInboundMessage`** | channel, external user id, text, event id | WhatsApp JSON |
| **`BlTurnResult`** | backend outcome (`recommend` / `clarify` / `refuse_scope` / `fail`), text, stays, duplicate | Graph API payload |
| **`BlOutboundMessage`** | recipient id + rendered English body | Graph API payload |
| **`IInboundReplyManager`** | One inbound message → at most one outbound reply | HTTP, WhatsApp JSON, Graph send |
| **`IBackendClient`** | `POST /turns` | JSON parse, assistant logic |
| **`TurnResponseParser`** | Backend `/turns` JSON → `BlTurnResult` | HTTP |
| **`IReplyRenderer`** | Outcome → English body (5 stays when recommend) | HTTP |
| **`IMessageSender`** | Deliver one text to a platform user | Backend |
| **WhatsApp adapter** | Verify webhook, parse Cloud API JSON, send via Graph | Shared core |

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
    infra/whatsapp/       parser, verifier, Graph sender, webhook handler
    http/                 WhatsAppWebhookController
```

## Layer graph

```
GET/POST /webhooks/whatsapp
  └── IWhatsAppWebhookHandler
        ├── IWebhookVerifier          (GET hub.challenge)
        ├── IInboundParser            Cloud JSON → 0..n BlInboundMessage
        ├── IInboundReplyManager      each text → Optional<BlOutboundMessage>
        │     ├── IBackendClient      POST localhost:8080/turns  channel=whatsapp
        │     │     └── TurnResponseParser   JSON body → BlTurnResult
        │     └── IReplyRenderer      English body
        └── IMessageSender            Graph messages API (only if a reply exists)
```

`duplicate=true` from the backend → **do not send** (M-10). Backend HTTP/network failure → still try to send a retry message on WhatsApp (M-09). If Graph send fails after the backend already succeeded, do not loop retries; a later webhook is skipped as duplicate (M-11).

**WhatsApp message path (step by step):** [whatsapp.md](whatsapp.md) — Meta verify GET, inbound POST, parse, `POST /turns`, render, Graph send, duplicates.

Inner docs:

- [inbound.md](inbound.md) — inbound reply manager, backend client, renderer
- [whatsapp.md](whatsapp.md) — Cloud API webhook, full WhatsApp flow, Graph send
